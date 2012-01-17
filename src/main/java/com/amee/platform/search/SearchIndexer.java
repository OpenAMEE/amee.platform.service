package com.amee.platform.search;

public interface SearchIndexer {

    /**
     * Clears the SearchIndexer's state.
     *
     */
    void clear();

    /**
     * Updates the index for the given SearchIndexerContext.
     *
     * @param documentContext the SearchIndexerContext describing the DataCategory to update.
     */
    void handleSearchIndexerContext(SearchIndexerContext documentContext);
}
