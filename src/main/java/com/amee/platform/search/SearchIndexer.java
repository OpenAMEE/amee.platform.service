package com.amee.platform.search;

public interface SearchIndexer {

    public void handleDocumentContext();

    public void setDocumentContext(SearchIndexerContext documentContext);
}
