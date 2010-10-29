package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

import java.util.Collection;

public interface LuceneService {

    ResultsWrapper<Document> doSearch(Query query, int resultStart, int resultLimit);

    ResultsWrapper<Document> doSearch(Query query);

    void deleteDocuments(Term... terms);

    public void deleteDocuments(Query q);

    void updateDocument(Document document, Term... terms);

    void addDocuments(Collection<Document> documents);

    void unlockIndex();

    public void closeEverything();

    void clearIndex();

    void addDocument(Document document);

    public void checkSearcher();

    public void flush();
}
