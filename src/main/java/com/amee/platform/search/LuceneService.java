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

    void updateDocument(Document document, Term... terms);

    void addDocuments(Collection<Document> documents);

    void unlockIndex();

    void clearIndex();

    void addDocument(Document document);
}
