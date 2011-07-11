package com.amee.platform.search;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class DirectoryTest {

    private org.apache.lucene.store.Directory simpleFsDir;
    private org.apache.lucene.store.Directory nioFsDir;
    private org.apache.lucene.store.Directory ramDir;
    private Collection<String> docs;

    // Change the following to adjust performance of indexing with FSDirectory

    /**
     * Determines how often segment indices are merged by addDocument().  With
     * smaller values, less RAM is used while indexing, and searches on
     * unoptimized indices are faster, but indexing speed is slower.  With larger
     * values, more RAM is used during indexing, and while searches on unoptimized
     * indices are slower, indexing is faster.  Thus larger values (> 10) are best
     * for batch index creation, and smaller values (< 10) for indices that are
     * interactively maintained.
     * This must never be less than 2.  The default value is 10.
     */
    private int mergeFactor = 10;

    /**
     * Returns the largest segment (measured by document count) that may be merged with other segments.
     * The default value is Integer.MAX_VALUE.
     */
    private int maxMergeDocs = Integer.MAX_VALUE;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {10},
            {12},
            {15},
            {19},
            {24},
            {50}});
    }

    public DirectoryTest(int mergeFactor) {
        this.mergeFactor = mergeFactor;
        docs = loadDocuments(30000, 5);
    }

    @Before
    public void setUp() throws Exception {
        String simpleFsIndexDir =
            System.getProperty("java.io.tmpdir", "tmp") + System.getProperty("file.separator") + "simplefs-index";
        simpleFsDir = new SimpleFSDirectory(new File(simpleFsIndexDir));
        String nioFsIndexDir =
            System.getProperty("java.io.tmpdir", "tmp") + System.getProperty("file.separator") + "niofs=index";
        nioFsDir = new NIOFSDirectory(new File(nioFsIndexDir));
        ramDir = new RAMDirectory();
    }

    /**
     * Times adding documents to the index.
     * 
     * @throws Exception
     */
    @Ignore("Used for performance testing of Directory types.")
    @Test
    public void testTiming() throws Exception {
        long simpleTiming = timeIndexWriter(simpleFsDir);
        long simpleSearchTiming = timeIndexSearcher(simpleFsDir);
        long nioTiming = timeIndexWriter(nioFsDir);
        long nioSearchTiming = timeIndexSearcher(nioFsDir);
        long ramTiming = timeIndexWriter(ramDir);
        long ramSearchTiming = timeIndexSearcher(ramDir);

        System.out.println("Using mergeFactor: " + mergeFactor);
        System.out.println("SimpleFSDirectory write time: " + (simpleTiming) + " ms");
        System.out.println("NIOFSDirectory write time: " + (nioTiming) + " ms");
        System.out.println("RamDirectory write time: " + (ramTiming) + " ms");
        System.out.println("---------------------------------------");
        System.out.println("SimpleFSDirectory search time: " + (simpleSearchTiming) + " ms");
        System.out.println("NIOFSDirectory search time: " + (nioSearchTiming) + " ms");
        System.out.println("RamDirectory search time: " + (ramSearchTiming) + " ms");
        System.out.println("***************************************");
    }

    private long timeIndexWriter(Directory dir) throws IOException {
        long start = System.currentTimeMillis();
        addDocuments(dir);
        long stop = System.currentTimeMillis();
        return (stop - start);
    }

    private long timeIndexSearcher(Directory dir) throws IOException {
        long start = System.currentTimeMillis();
        doSearch(dir);
        long stop = System.currentTimeMillis();
        return (stop - start);
    }

    private void doSearch(Directory dir) throws IOException {
        IndexSearcher simpleSearcher = new IndexSearcher(dir);
        Term term = new Term("text", "bibamus");
        Query query = new TermQuery(term);
        TopDocs topdocs = simpleSearcher.search(query, 100);
        assertTrue(topdocs.totalHits > 0);
    }

    /**
     * Create an index and add Documents to it.
     * 
     * @param dir
     * @throws IOException
     */
    private void addDocuments(Directory dir) throws IOException {
        IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        writer.setMergeFactor(mergeFactor);
        writer.setMaxMergeDocs(maxMergeDocs);

        for (String field : docs) {
            Document document = new Document();

            // Not analyzed. Indexed. Stored verbatim.
            // Index the field's value without using an Analyzer, so it can be searched.
            // As no analyzer is used the value will be stored as a single term. Useful for non-text fields, e.g. date or url.
            document.add(new Field("keyword", field, Field.Store.YES, Field.Index.NOT_ANALYZED));

            // Not analyzed. Not indexed. Stored verbatim.
            // Do not index the field value. This field can thus not be searched,
            // but one can still access its contents provided it is stored.
            document.add(new Field("unindexed", field, Field.Store.YES, Field.Index.NO));

            // Analyzed. Indexed. Not stored.
            // Do not store the field value in the index.
            document.add(new Field("unstored", field, Field.Store.NO, Field.Index.ANALYZED));

            // Analyzed. Indexed. Stored.
            document.add(new Field("text", field, Field.Store.YES, Field.Index.ANALYZED));

            writer.addDocument(document);
        }
        writer.optimize();
        writer.close();
    }

    /**
     * Generate a list of Strings to be indexed.
     * @param numDocs
     * @param wordsPerDoc
     * @return
     */
    private Collection<String> loadDocuments(int numDocs, int wordsPerDoc) {
        Collection docs = new ArrayList<String>(numDocs);
        for (int i = 0; i < numDocs; i++) {
            StringBuffer doc = new StringBuffer(wordsPerDoc);
            for (int j = 0; j < wordsPerDoc; j++) {
                doc.append("Bibamus ");
            }
            docs.add(doc.toString());
        }
        return docs;
    }
}
