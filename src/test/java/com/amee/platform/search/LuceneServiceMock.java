package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

import java.util.*;

public class LuceneServiceMock implements LuceneService {

    private ResultsWrapper<Document> allResults;
    private ResultsWrapper<Document> resultsWrapperA;
    private ResultsWrapper<Document> resultsWrapperB;
    private List<Document> allDocuments;
    private int count = 0;

    @Override
    public ResultsWrapper<Document> doSearch(Query query, int resultStart, int resultLimit) {
        if (count == 0) {
            count++;
            return resultsWrapperA;
        } else if (count > 2) {
            count++;
            String entityUid = "";
            String entityType = "";
            Set<Term> terms = new HashSet<Term>();
            query.extractTerms(terms);
            for (Term t : terms) {
                if (t.field().equals("entityUid")) {
                    entityUid = t.text();
                } else if (t.field().equals("entityType")) {
                    entityType = t.text();
                }
            }
            for (Document d : allDocuments) {
                if (d.getField("entityUid").stringValue().equals(entityUid) &&
                        d.getField("entityType").stringValue().equals(entityType)) {
                    return new ResultsWrapper<Document>(new ArrayList<Document>(Arrays.asList(d)), false);
                }
            }
            return new ResultsWrapper<Document>(new ArrayList<Document>(), false);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ResultsWrapper<Document> doSearch(Query query) {
        if (count == 1) {
            count++;
            return resultsWrapperB;
        } else if (count == 2) {
            count++;
            if (allResults != null) {
                return allResults;
            } else {
                return new ResultsWrapper<Document>(new ArrayList<Document>(), false);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void deleteDocuments(Term... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDocument(Document document, Term... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDocuments(Collection<Document> documents) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlockIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDocument(Document document) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkSearcher() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    public ResultsWrapper<Document> getAllResults() {
        return allResults;
    }

    public void setAllResults(ResultsWrapper<Document> allResults) {
        this.allResults = allResults;
    }

    public ResultsWrapper<Document> getResultsWrappeAr() {
        return resultsWrapperA;
    }

    public void setResultsWrapperA(ResultsWrapper<Document> resultsWrapperA) {
        this.resultsWrapperA = resultsWrapperA;
    }

    public ResultsWrapper<Document> getResultsWrapperB() {
        return resultsWrapperB;
    }

    public void setResultsWrapperB(ResultsWrapper<Document> resultsWrapperB) {
        this.resultsWrapperB = resultsWrapperB;
    }

    public List<Document> getAllDocuments() {
        return allDocuments;
    }

    public void setAllDocuments(List<Document> allDocuments) {
        this.allDocuments = allDocuments;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
