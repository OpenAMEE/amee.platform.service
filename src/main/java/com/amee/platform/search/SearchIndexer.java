package com.amee.platform.search;

public interface SearchIndexer {

    void clear();

    void handleSearchIndexerContext(SearchIndexerContext documentContext);
}
