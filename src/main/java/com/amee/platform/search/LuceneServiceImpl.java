package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockReleaseFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LuceneIndexWrapper wraps a Lucene file system index and provides a simplified abstraction of
 * the Lucene API.
 */
public class LuceneServiceImpl implements LuceneService {

    private final Log log = LogFactory.getLog(getClass());

    public final static int MAX_NUM_HITS = 1000;

    /**
     * Path to the SnapShooter script.
     */
    private String snapShooterPath = "";

    /**
     * Path to the dir containing lucene index and snapshots.
     */
    private String indexPath = "";

    /**
     * Path to the dir of the current lucene index. Usually this is: indexPath + '/lucene'
     */
    private String lucenePath = "";

    /**
     * The primary Lucene Searcher.
     */
    private volatile IndexSearcher searcher;

    /**
     * A Lucene Searcher used recently. A reference is kept to this when the primary Searcher
     * is re-opened.
     */
    private IndexSearcher lastSearcher;

    /**
     * The shared Lucene Analyzer.
     */
    private volatile Analyzer analyzer;

    /**
     * The shared Lucene directory.
     */
    private volatile Directory directory;

    /**
     * The shared Lucene IndexWriter.
     */
    private volatile IndexWriter indexWriter;

    /**
     * Is this instance the master index node? There can be only one!
     */
    private boolean masterIndex = false;

    /**
     * Should the search index be cleared on application start?
     */
    private boolean clearIndex = false;

    /**
     * Should index snapshots be created? Only required for replication.
     */
    private Boolean snapshotEnabled = false;

    /**
     * The time of the most recent index write.
     */
    private long lastWriteTime = 0L;

    /**
     * Should Searcher be checked after every commit? Useful for development & testing.
     */
    private boolean checkSearcherOnCommit = false;

    /**
     * Lock objects for the index.
     */
    private ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private Lock rLock = rwLock.readLock();
    private Lock wLock = rwLock.writeLock();

    /**
     * Conduct a search in the Lucene index based on the supplied Query, constrained by resultStart and resultLimit.
     * <p/>
     * At most this will allow up to MAX_RESULT_LIMIT search hits, with a return window based
     * on resultStart and resultLimit.
     *
     * @param query       to search with
     * @param resultStart 0 based index of first result
     * @param resultLimit results limit
     * @return a List of Lucene Documents
     */
    @Override
    public ResultsWrapper<Document> doSearch(Query query, final int resultStart, final int resultLimit) {
        return doSearch(query, resultStart, resultLimit, MAX_NUM_HITS);
    }

    @Override
    public ResultsWrapper<Document> doSearch(Query query, final int resultStart, final int resultLimit, final int maxNumHits) {
        return doSearch(query, resultStart, resultLimit, maxNumHits, Sort.RELEVANCE);
    }

    /**
     * Conduct a search in the Lucene index based on the supplied Query, constrained by resultStart and resultLimit.
     * <p/>
     * At most this will allow up to MAX_RESULT_LIMIT search hits, with a return window based
     * on resultStart and resultLimit.
     *
     * @param query       to search with
     * @param resultStart 0 based index of first result
     * @param resultLimit results limit
     * @param maxNumHits  maximum number of hits to return
     * @param sortField   Sort object to sort by. This field must be indexed but not tokenized.
     * @return a List of Lucene Documents
     */
    @Override
    public ResultsWrapper<Document> doSearch(Query query, final int resultStart, final int resultLimit, final int maxNumHits, Sort sortField) {

        rLock.lock();

        try {

            // Log time.
            log.info("doSearch() query='" + query.toString() + "', resultStart=" + resultStart + ", resultLimit=" + resultLimit);
            long start = System.currentTimeMillis();

            // Cannot go above maxNumHits.
            int numHits = resultStart + resultLimit;
            if (numHits > maxNumHits) {
                numHits = maxNumHits;
            }

            // Get Collector limited to numHits + 1, so we can detect truncations.
            TopFieldCollector collector = TopFieldCollector.create(sortField, numHits + 1, false, false, false, false);

            // Get the IndexSearcher and do the search.
            Searcher searcher = getIndexSearcher();
            searcher.search(query, collector);

            // Get hits within our start and limit range.
            ScoreDoc[] hits;
            hits = collector.topDocs(resultStart, resultLimit + 1).scoreDocs;

            // Assemble List of Documents.
            List<Document> documents = new ArrayList<Document>();
            for (ScoreDoc hit : hits) {
                documents.add(searcher.doc(hit.doc));
            }

            // Trim resultLimit if we're close to maxNumHits.
            int resultLimitWithCeiling = resultLimit;
            if (resultStart >= maxNumHits) {
                // Never return results.
                resultLimitWithCeiling = 0;
            } else if ((resultStart + resultLimit) > maxNumHits) {
                // Only return those results from resultStart to maxNumHits.
                resultLimitWithCeiling = maxNumHits - resultStart;
            }

            // Create ResultsWrapper appropriate for our limit.
            int totalHits = collector.getTotalHits();
            ResultsWrapper<Document> results = new ResultsWrapper<Document>(
                    documents.size() > resultLimitWithCeiling ? documents.subList(0, resultLimitWithCeiling) : documents,
                    (documents.size() > resultLimitWithCeiling) && !((resultStart + resultLimitWithCeiling) >= maxNumHits),
                    resultStart,
                    resultLimit,
                    totalHits > maxNumHits ? maxNumHits : totalHits);

            // Log time and return.
            log.info("doSearch() Duration: " + (System.currentTimeMillis() - start));
            return results;

        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            rLock.unlock();
        }
    }

    /**
     * Conduct a search in the Lucene index based on the supplied Query (unconstrained.
     * <p/>
     * At most this will allow up to MAX_RESULT_LIMIT search hits.
     *
     * @param query to search with
     * @return a List of Lucene Documents
     */
    @Override
    public ResultsWrapper<Document> doSearch(Query query) {
        return doSearch(query, MAX_NUM_HITS);
    }

    /**
     * Conduct a search in the Lucene index based on the supplied Query (unconstrained.
     * <p/>
     * At most this will allow up to MAX_RESULT_LIMIT search hits.
     *
     * @param query to search with
     * @return a List of Lucene Documents
     */
    @Override
    public ResultsWrapper<Document> doSearch(Query query, final int maxNumHits) {
        rLock.lock();
        try {
            log.info("doSearch() query='" + query.toString() + "'");
            long start = System.currentTimeMillis();
            // Get Collector limited to numHits + 1, so we can detect truncations.
            TopScoreDocCollector collector = TopScoreDocCollector.create(maxNumHits + 1, true);
            // Get the IndexSearcher and do the search.
            Searcher searcher = getIndexSearcher();
            searcher.search(query, collector);
            // Get all hits.
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            // Assemble List of Documents.
            List<Document> documents = new ArrayList<Document>();
            for (ScoreDoc hit : hits) {
                documents.add(searcher.doc(hit.doc));
            }
            // Create ResultsWrapper containing all Documents.
            ResultsWrapper<Document> results = new ResultsWrapper<Document>(
                    documents,
                    documents.size() > maxNumHits);
            log.info("doSearch() Duration: " + (System.currentTimeMillis() - start));
            return results;
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public void addDocument(Document document) {
        if (!masterIndex || (document == null)) return;
        rLock.lock();
        try {
            getIndexWriter().addDocument(document);
            getIndexWriter().commit();
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            lastWriteTime = System.currentTimeMillis();
            rLock.unlock();
        }
        if (checkSearcherOnCommit) {
            checkSearcher();
        }
    }

    @Override
    public void addDocuments(Collection<Document> documents) {
        if (!masterIndex || (documents == null) || documents.isEmpty()) return;
        rLock.lock();
        try {
            for (Document document : documents) {
                getIndexWriter().addDocument(document);
            }
            getIndexWriter().commit();
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            lastWriteTime = System.currentTimeMillis();
            rLock.unlock();
        }
        if (checkSearcherOnCommit) {
            checkSearcher();
        }
    }

    /**
     * This will remove the Document matching the supplied Terms and then add the supplied Document.
     *
     * @param document to add
     * @param terms    Terms to form Query to remove existing Document.
     */
    @Override
    public void updateDocument(Document document, Term... terms) {
        if (!masterIndex || (document == null)) return;
        BooleanQuery q = new BooleanQuery();
        for (Term t : terms) {
            if (t != null) {
                q.add(new TermQuery(t), BooleanClause.Occur.MUST);
            }
        }
        rLock.lock();
        try {
            getIndexWriter().deleteDocuments(q);
            getIndexWriter().addDocument(document);
            getIndexWriter().commit();
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            lastWriteTime = System.currentTimeMillis();
            rLock.unlock();
        }
        if (checkSearcherOnCommit) {
            checkSearcher();
        }
    }

    @Override
    public void deleteDocuments(Term... terms) {
        if (!masterIndex) return;
        BooleanQuery q = new BooleanQuery();
        for (Term t : terms) {
            if (t != null) {
                q.add(new TermQuery(t), BooleanClause.Occur.MUST);
            }
        }
        deleteDocuments(q);
    }

    @Override
    public void deleteDocuments(Query q) {
        if (!masterIndex) return;
        rLock.lock();
        try {
            getIndexWriter().deleteDocuments(q);
            getIndexWriter().commit();
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            lastWriteTime = System.currentTimeMillis();
            rLock.unlock();
        }
        if (checkSearcherOnCommit) {
            checkSearcher();
        }
    }

    /**
     * Prepare the Lucene index. Unlock it and potentially clear it, depending on the amee.clearIndex system
     * property
     */
    @Override
    public void prepareIndex() {
        unlockIndex();
        if (clearIndex) {
            clearIndex();
        }
    }

    /**
     * Clear the Lucene index.
     */
    private void clearIndex() {
        if (!masterIndex) return;
        wLock.lock();
        try {
            // Ensure everything is closed.
            closeEverything();
            // Ensure index is not locked (perhaps from a crash).
            unlockIndex();
            // Create a new index.
            IndexWriter indexWriter = getNewIndexWriter(true);
            // Close the index.
            indexWriter.commit();
            indexWriter.close();
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            closeIndexWriter();
            wLock.unlock();
        }
        if (checkSearcherOnCommit) {
            checkSearcher();
        }
    }

    /**
     * Unlocks the Lucene index. Useful following JVM crashes.
     */
    private void unlockIndex() {
        wLock.lock();
        try {
            if (IndexReader.indexExists(getDirectory())) {
                if (IndexWriter.isLocked(getDirectory())) {
                    log.info("unlockIndex() Unlocking index.");
                    IndexWriter.unlock(getDirectory());
                }
            }
        } catch (LockReleaseFailedException e) {
            log.warn("unlockIndex() Caught LockReleaseFailedException: " + e.getMessage());
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        } finally {
            wLock.unlock();
        }
    }

    /**
     * On finalize, ensure Lucene objects are closed.
     *
     * TODO: Should we be using finalize()? http://www.codeguru.com/java/tij/tij0051.shtml
     * 
     * @throws Throwable the <code>Exception</code> raised by this method
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeEverything();
    }

    /**
     * Ensure all Lucene objects are closed.
     */
    @Override
    public void closeEverything() {
        wLock.lock();
        try {
            closeLastSearcher();
            closeSearcher();
            closeIndexWriter();
            unlockIndex();
            closeDirectory();
        } finally {
            wLock.unlock();
        }
    }

    /**
     * Get the Searcher.
     *
     * @return the Searcher
     */
    private IndexSearcher getIndexSearcher() {

        // Note the usage of the local variable result which seems unnecessary.
        // For some versions of the Java VM, it will make the code 25% faster and for others, it won't hurt.
        // Joshua Bloch "Effective Java, Second Edition", p. 283
        IndexSearcher result = searcher;
        if (result == null) {
            synchronized (this) {
                result = searcher;
                if (result == null) {
                    try {
                        searcher = result = new IndexSearcher(getDirectory(), true);
                    } catch (ClosedByInterruptException e) {
                        throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
                    } catch (IOException e) {
                        throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Check the Searcher to see if it needs re-opening.
     * <p/>
     * This method is called via cron.
     */
    @Override
    public void checkSearcher() {
        try {
            // Close the last Searcher.
            closeLastSearcher();
            // Should the current Searcher be re-opened?
            if ((searcher != null) && !searcher.getIndexReader().isCurrent()) {
                // Store the current Searcher so it can be closed later.
                // This allows other threads using the Searcher to finish their work.
                lastSearcher = searcher;
                // Set the Searcher to null means a new instance will be created later in getSearcher().
                searcher = null;
            }
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Close the Searcher.
     */
    private void closeSearcher() {
        if (searcher == null) return;
        try {
            searcher.close();
            searcher = null;
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Close the last Searcher.
     */
    private void closeLastSearcher() {
        if (lastSearcher == null) return;
        try {
            lastSearcher.close();
            lastSearcher = null;
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Get the Analyzer. Will call getNewAnalyzer if it does not yet exist.
     *
     * @return the Analyzer
     */
    private Analyzer getAnalyzer() {

        // Note the usage of the local variable result which seems unnecessary.
        // For some versions of the Java VM, it will make the code 25% faster and for others, it won't hurt.
        // Joshua Bloch "Effective Java, Second Edition", p. 283
        Analyzer result = analyzer;
        if (result == null) {
            synchronized (this) {
                result = analyzer;
                if (result == null) {
                    analyzer = result = new StandardAnalyzer(Version.LUCENE_30);
                }
            }
        }
        return result;
    }

    /**
     * Gets the Directory. Will call createDirectory if it does not yet exist.
     *
     * Creates a {@link SimpleFSDirectory} rather than using FSDirectory.open because
     * {@link com.amee.base.resource.LocalResourceHandler#handleWithTimeout} can cause Exceptions.
     * 
     * @return the Directory
     */
    private Directory getDirectory() {

        // Note the usage of the local variable result which seems unnecessary.
        // For some versions of the Java VM, it will make the code 25% faster and for others, it won't hurt.
        // Joshua Bloch "Effective Java, Second Edition", p. 283
        Directory result = directory;
        if (result == null) {
            synchronized (this) {
                result = directory;
                if (result == null) {
                    try {
                        directory = result = new SimpleFSDirectory(new File(lucenePath));
                    } catch (ClosedByInterruptException e) {
                        throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
                    } catch (IOException e) {
                        throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Closes the Lucene directory.
     */
    private synchronized void closeDirectory() {
        if (directory == null) return;
        try {
            directory.close();
            directory = null;
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Gets IndexWriter. Will call getNewIndexWriter if an IndexWriter is not yet created. Can
     * be called multiple times within a thread.
     * <p/>
     * Later, closeIndexWriter must be called at least once.
     *
     * @return the IndexWriter
     */
    private IndexWriter getIndexWriter() {

        // Note the usage of the local variable result which seems unnecessary.
        // For some versions of the Java VM, it will make the code 25% faster and for others, it won't hurt.
        // Joshua Bloch "Effective Java, Second Edition", p. 283
        IndexWriter result = indexWriter;
        if (result == null) {
            synchronized (this) {
                result = indexWriter;
                if (result == null) {
                    indexWriter = result = getNewIndexWriter(false);
                }
            }
        }
        return result;
    }

    /**
     * Construct and set a new IndexWriter. Should only be called once within a thread.
     * <p/>
     * Later, closeIndexWriter must be called at least once.
     *
     * @param create a new index if true
     * @return IndexWriter
     */
    private IndexWriter getNewIndexWriter(boolean create) {
        try {
            return new IndexWriter(
                    getDirectory(),
                    getAnalyzer(),
                    create,
                    IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Flush the IndexWriter. Will optimise and commit the index if appropriate.
     */
    @Override
    public void flush() {
        if (!masterIndex) return;
        rLock.lock();
        try {
            log.info("flush() Starting.");
            if (!getIndexSearcher().getIndexReader().isOptimized()) {
                getIndexWriter().optimize();
                getIndexWriter().commit();
            } else {
                log.info("flush() Index already optimized.");
            }
            log.info("flush() Done.");
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("flush() Caught IOException: " + e.getMessage(), e);
        } finally {
            rLock.unlock();
        }
        if (checkSearcherOnCommit) {
            checkSearcher();
        }
    }

    /**
     * Closes the IndexWriter. Will flush the index prior to closing.
     */
    private synchronized void closeIndexWriter() {
        if (indexWriter == null) return;
        log.info("closeIndexWriter()");
        try {
            flush();
            indexWriter.close();
            indexWriter = null;
        } catch (ClosedByInterruptException e) {
            throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Caught IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Takes a snapshot of the lucene index using the Solr SnapShooter shell script.
     * http://wiki.apache.org/solr/SolrCollectionDistributionScripts
     */
    @Override
    public void takeSnapshot() {
        if (!snapshotEnabled) return;
        Process p = null;
        Timer timer;
        String command;
        InterruptTimerTask interrupter;
        // Only take a snapshot if it is due.
        if (isSnapshotDue()) {
            // We need a write lock to ensure consistency.
            wLock.lock();
            // Setup time and command.
            timer = new Timer(true);
            command = snapShooterPath + " -d " + indexPath;
            try {
                log.info("takeSnapshot() Executing: " + command);
                // Invoke the SnapShooter.
                p = Runtime.getRuntime().exec(command);
                // Use a Timer to interrupt later on timeout.
                interrupter = new InterruptTimerTask(Thread.currentThread());
                timer.schedule(interrupter, 30 * 1000); // 30 second timeout.
                // Wait for process to complete (or until timeout is reached).
                p.waitFor();
                // If we get here then the snapshot completed.
                log.info("takeSnapshot() Done.");
            } catch (ClosedByInterruptException e) {
                throw new LuceneServiceException("Caught ClosedByInterruptException: " + e.getMessage(), e);
            } catch (IOException e) {
                log.error("takeSnapshot() Caught IOException: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                p.destroy();
                log.warn("takeSnapshot() Timed out.");
            } finally {
                // Tidy up.
                timer.cancel();
                Thread.interrupted();
                wLock.unlock();
            }
        }
    }

    /**
     * A TimerTask used by takeSnapshot().
     */
    private static class InterruptTimerTask extends TimerTask {

        private Thread thread;

        public InterruptTimerTask(Thread t) {
            this.thread = t;
        }

        public void run() {
            thread.interrupt();
        }

    }

    /**
     * @return true if the last snapshot was taken longer than the most recent write
     */
    private boolean isSnapshotDue() {
        // Unless this is the master index this test will always return false.
        return lastWriteTime > getLastSnapshotTime();
    }

    /**
     * Gets the last modified time of the latest snapshot.
     *
     * @return A long value representing the time the snapshot was taken,
     *         measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970),
     *         or 0L if no snapshot exists or if an I/O error occurs.
     */
    private long getLastSnapshotTime() {
        File snapshotDir = new File(indexPath);
        File[] snapshotFiles = snapshotDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("snapshot");
            }
        });
        if (snapshotFiles != null && snapshotFiles.length > 0) {
            Arrays.sort(snapshotFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            return snapshotFiles[0].lastModified();
        } else {
            return 0L;
        }
    }

    @Value("#{ systemProperties['amee.masterIndex'] }")
    public void setMasterIndex(Boolean masterIndex) {
        this.masterIndex = masterIndex;
    }

    @Override
    public boolean getClearIndex() {
        return clearIndex;
    }

    @Override
    @Value("#{ systemProperties['amee.clearIndex'] }")
    public void setClearIndex(Boolean clearIndex) {
        this.clearIndex = clearIndex;
    }

    @Value("#{ systemProperties['amee.snapshotEnabled'] }")
    public void setSnapshotEnabled(Boolean snapshotEnabled) {
        this.snapshotEnabled = snapshotEnabled;
    }

    @Value("#{ systemProperties['amee.indexPath'] }")
    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    @Value("#{ systemProperties['amee.lucenePath'] }")
    public void setLucenePath(String lucenePath) {
        this.lucenePath = lucenePath;
    }

    @Value("#{ systemProperties['amee.snapShooterPath'] }")
    public void setSnapShooterPath(String snapShooterPath) {
        this.snapShooterPath = snapShooterPath;
    }

    @Value("#{ systemProperties['amee.indexCheckSearcherOnCommit'] }")
    public void setCheckSearcherOnCommit(Boolean checkSearcherOnCommit) {
        this.checkSearcherOnCommit = checkSearcherOnCommit;
    }
}