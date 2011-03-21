package com.amee.platform.search;

public interface SearchIndexer {

    public void clear();

    public void handleSearchIndexerContext(SearchIndexerContext documentContext);
}
