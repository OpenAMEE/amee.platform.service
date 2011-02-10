package com.amee.platform.search;

import org.springframework.beans.factory.annotation.Autowired;

public class SearchIndexerRunner implements Runnable {

    @Autowired
    private SearchIndexer searchIndexer;

    // A wrapper object encapsulating the context of the current indexing operation.
    private SearchIndexerContext documentContext;

    @Override
    public void run() {
        searchIndexer.setDocumentContext(documentContext);
        searchIndexer.handleDocumentContext();
    }

    public void setDocumentContext(SearchIndexerContext documentContext) {
        this.documentContext = documentContext;
    }
}
