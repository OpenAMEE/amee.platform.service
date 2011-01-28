package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import java.util.Collection;

public interface LuceneService {

    public ResultsWrapper<Document> doSearch(Query query, int resultStart, int resultLimit);

    public ResultsWrapper<Document> doSearch(Query query, int resultStart, int resultLimit, int maxNumHits);

    public ResultsWrapper<Document> doSearch(Query query, final int resultStart, final int resultLimit, final int maxNumHits, Sort sortField);

    public ResultsWrapper<Document> doSearch(Query query);

    public ResultsWrapper<Document> doSearch(Query query, int maxNumHits);

    public void deleteDocuments(Term... terms);

    public void deleteDocuments(Query q);

    public void updateDocument(Document document, Term... terms);

    public void addDocuments(Collection<Document> documents);

    public void unlockIndex();

    public void closeEverything();

    public void clearIndex();

    public void addDocument(Document document);

    public void checkSearcher();

    public void flush();

    public void takeSnapshot();
}
