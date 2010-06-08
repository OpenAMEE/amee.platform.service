package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import com.amee.base.resource.ValidationResult;
import com.amee.base.validation.ValidationException;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * LuceneIndexWrapper wraps a Lucene file system index and provides a simplified abstraction of
 * the Lucene API.
 * <p/>
 * Instances of this class should only be used by one thread. Care should be taken to respect the
 * sequence of method calls as described in the method documentation.
 */
public class LuceneService implements Serializable {

    private final Log log = LogFactory.getLog(getClass());

    public final static int MAX_NUM_HITS = 1000;

    /**
     * Path to the snapshooter script
     */
    private String snapShooterPath = "";

    /**
     * Path to the dir containing lucene index
     */
    private String indexDirPath = "";

    /**
     * Number of seconds to wait until taking a new snapshot
     */
    private int snapTime = 0;

    private Analyzer analyzer;
    private Directory directory;
    private IndexWriter indexWriter;

    /**
     * Is this instance the master index node? There can be only one!
     */
    private boolean masterIndex = false;

    @Value("#{ systemProperties['amee.masterIndex'] }")
    public void setMasterIndex(Boolean masterIndex) {
        this.masterIndex = masterIndex;
    }

    public String getIndexDirPath() {
        return indexDirPath;
    }

    public void setIndexDirPath(String indexDirPath) {
        this.indexDirPath = indexDirPath;
    }

    public String getSnapShooterPath() {
        return snapShooterPath;
    }

    public void setSnapShooterPath(String snapShooterPath) {
        this.snapShooterPath = snapShooterPath;
    }

    public int getSnapTime() {
        return snapTime;
    }

    public void setSnapTime(int snapTime) {
        this.snapTime = snapTime;
    }

    /**
     * Conduct a search in the Lucene index based on the supplied field name and query string.
     *
     * @param field name of field to search within
     * @param q     query to search with
     * @return a List of Lucene Documents
     */
    public ResultsWrapper<Document> doSearch(String field, String q) {
        log.debug("doSearch()");
        QueryParser parser = new QueryParser(Version.LUCENE_30, field, getAnalyzer());
        try {
            return doSearch(parser.parse(q));
        } catch (ParseException e) {
            ValidationResult validationResult = new ValidationResult();
            validationResult.addValue("field", field);
            validationResult.addValue("query", q);
            validationResult.addError(field, "parse", q);
            throw new ValidationException(validationResult);
        }
    }

    public ResultsWrapper<Document> doSearch(Query query) {
        return doSearch(query, 0, 50);
    }

    /**
     * Conduct a search in the Lucene index based on the supplied Query.
     * <p/>
     * At most this will allow up to MAX_RESULT_LIMIT search hits, with a return window based
     * on resultStart and resultLimit.
     *
     * @param query       to search with
     * @param resultStart 0 based index of first result
     * @param resultLimit results limit
     * @return a List of Lucene Documents
     */
    public ResultsWrapper<Document> doSearch(Query query, int resultStart, int resultLimit) {
        log.debug("doSearch()");
        try {
            // Cannot go above MAX_NUM_HITS.
            int numHits = resultStart + resultLimit;
            if (numHits > MAX_NUM_HITS) {
                numHits = MAX_NUM_HITS;
            }
            // Searcher for our index.
            IndexSearcher searcher = new IndexSearcher(getDirectory());
            // Get Collector limited to numHits + 1, so we can detect truncations.  
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits + 1, true);
            searcher.search(query, collector);
            // Get hits within our start and limit range.
            ScoreDoc[] hits = collector.topDocs(resultStart, resultLimit + 1).scoreDocs;
            // Assemble List of Documents.
            List<Document> documents = new ArrayList<Document>();
            for (ScoreDoc hit : hits) {
                documents.add(searcher.doc(hit.doc));
            }
            searcher.close();
            // Trim resultLimit if we're close to MAX_NUM_HITS.
            if (resultStart >= MAX_NUM_HITS) {
                // Never return results.
                resultLimit = 0;
            } else if ((resultStart + resultLimit) > MAX_NUM_HITS) {
                // Only return those results from resultStart to MAX_NUM_HITS. 
                resultLimit = MAX_NUM_HITS - resultStart;
            }
            // Create ResultsWrapper appropriate for our limit.
            return new ResultsWrapper<Document>(
                    documents.size() > resultLimit ? documents.subList(0, resultLimit) : documents,
                    documents.size() > resultLimit);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    public void addDocument(Document document) {
        synchronized (getIndexWriter()) {
            try {
                getIndexWriter().addDocument(document);
                closeIndexWriter();
            } catch (IOException e) {
                throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
            }
        }
    }

    public void addDocuments(Collection<Document> documents) {
        synchronized (getIndexWriter()) {
            try {
                for (Document document : documents) {
                    getIndexWriter().addDocument(document);
                }
                closeIndexWriter();
            } catch (IOException e) {
                throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
            }
        }
    }

    public void updateDocument(Term term, Document document, Analyzer analyzer) {

        // Updates should only be performed on the master node.
        if (masterIndex) {
            synchronized (getIndexWriter()) {
                try {
                    getIndexWriter().updateDocument(term, document, analyzer);
                    closeIndexWriter();
                } catch (IOException e) {
                    throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
                }
            }
        }
    }

    public void deleteDocuments(Term... terms) {
        BooleanQuery q = new BooleanQuery();
        for (Term t : terms) {
            q.add(new TermQuery(t), BooleanClause.Occur.MUST);
        }
        synchronized (getIndexWriter()) {
            try {
                getIndexWriter().deleteDocuments(q);
                closeIndexWriter();
            } catch (IOException e) {
                throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Clear the Lucene index.
     */
    public synchronized void clearIndex() {
        // First ensure index is not locked (perhaps from a crash).
        unlockIndex();
        // Create a new index.
        getIndexWriter(true);
        closeIndexWriter();
    }

    /**
     * Unlocks the Lucene index. Useful following JVM crashes.
     */
    public synchronized void unlockIndex() {
        try {
            if (IndexReader.indexExists(getDirectory())) {
                IndexReader.open(getDirectory());
                if (IndexWriter.isLocked(getDirectory())) {
                    IndexWriter.unlock(getDirectory());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Ensure the Lucene directory is closed
     *
     * @throws Throwable the <code>Exception</code> raised by this method
     */
    protected void finalize() throws Throwable {
        super.finalize();
        closeIndexWriter();
        closeDirectory();
    }

    /**
     * Get the Analyzer. Will call createAnalyser if it does not yet exist.
     *
     * @return the Analyzer
     */
    public synchronized Analyzer getAnalyzer() {
        if (analyzer == null) {
            createAnalyser();
        }
        return analyzer;
    }

    /**
     * Create a new Analyzer.
     */
    private void createAnalyser() {
        setAnalyzer(new StandardAnalyzer(Version.LUCENE_30));
    }

    /**
     * Set the Analyzer.
     *
     * @param analyzer to set
     */
    private void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Get the Directory. Will call createDirectory if it does not yet exist.
     *
     * @return the Directory
     */
    private synchronized Directory getDirectory() {
        if (directory == null) {
            createDirectory();
        }
        return directory;
    }

    /**
     * Open the Directory.
     */
    private void createDirectory() {
        try {
            String path = System.getProperty("amee.lucenePath", "/var/www/apps/platform-back/lucene/index");
            setDirectory(FSDirectory.open(new File(path)));
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the Lucene directory.
     */
    private synchronized void closeDirectory() {
        try {
            if (directory != null) {
                directory.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Set the Directory.
     *
     * @param directory to set
     */
    private void setDirectory(Directory directory) {
        this.directory = directory;
    }


    /**
     * Gets the current IndexWriter or creates a new one.
     *
     * @return the IndexWriter
     */
    private IndexWriter getIndexWriter() {
        return getIndexWriter(false);
    }

    /**
     * Gets IndexWriter. Will call createIndexWriter if an IndexWriter is not yet created. Can
     * be called multiple times within a thread. The create parameter is only effective when the
     * IndexWriter has not previously been created.
     * <p/>
     * Later, closeIndexWriter must be called at least once.
     *
     * @param create a new index if true
     * @return the IndexWriter
     */
    private synchronized IndexWriter getIndexWriter(boolean create) {
        if (indexWriter == null) {
            createIndexWriter(create);
        }
        return indexWriter;
    }

    /**
     * Construct and set a new IndexWriter. Should only be called once within a thread.
     * <p/>
     * Later, closeIndexWriter must be called at least once.
     *
     * @param create a new index if true
     */
    private void createIndexWriter(boolean create) {
        try {
            setIndexWriter(new IndexWriter(
                    getDirectory(),
                    getAnalyzer(),
                    create,
                    new IndexWriter.MaxFieldLength(25000)));
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the IndexWriter. Will optimise the index prior to closing.
     * <p/>
     * This must be called at least once following previous calls to getIndexWriter.
     */
    private synchronized void closeIndexWriter() {
        if (indexWriter != null) {
            try {
                indexWriter.optimize();
                indexWriter.commit();

                if (pastSnapTime()) {
                    log.debug("Passed time threshold for snapshot.");
                    takeSnapshot();
                }

                indexWriter.close();
                setIndexWriter(null);
            } catch (IOException e) {
                throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Set the IndexWriter.
     *
     * @param indexWriter to set
     */
    private void setIndexWriter(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    /**
     * Takes a snapshot of the lucene index using the solr snapshooter shell script.
     * http://wiki.apache.org/solr/SolrCollectionDistributionScripts
     */
    public void takeSnapshot() {
        String command = getSnapShooterPath() + " -d " + getIndexDirPath();
        log.debug("takeSnapshot() - executing " + command);

        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * @return true if the last snapshot was taken longer than snapTime seconds ago.
     */
    private boolean pastSnapTime() {
        Date now = new Date();
        return (now.getTime() - getLastSnapshotTime()) > (getSnapTime() * 1000);

    }

    /**
     * Gets the last modified time of the latest snapshot.
     *
     * @return A long value representing the time the snapshot was taken,
     *         measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970),
     *         or 0L if no snapshot exists or if an I/O error occurs.
     */
    private long getLastSnapshotTime() {
        File snapshotDir = new File(getIndexDirPath());
        File[] snapshotFiles = snapshotDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("snapshot");
            }
        });

        if (snapshotFiles.length > 0) {
            Arrays.sort(snapshotFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            return snapshotFiles[0].lastModified();
        } else {
            return 0L;
        }
    }
}
