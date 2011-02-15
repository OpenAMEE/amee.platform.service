package com.amee.platform.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SearchIndexerRunner implements Runnable {

    private final Log log = LogFactory.getLog(getClass());

    // A Set of Data Category UIDs that are currently being executed. This is used by the execute method
    // to ensure the same category is not indexed concurrently whilst allowing later indexing attempts
    // to eventually succeed.
    private final static Set<String> CURRENT_CATEGORY_UIDS = Collections.synchronizedSet(new HashSet<String>());

    @Autowired
    private SearchIndexer searchIndexer;

    @Autowired
    @Qualifier("searchIndexerTaskExecutor")
    private TaskExecutor taskExecutor;

    // A wrapper object encapsulating the context of the current indexing operation.
    private SearchIndexerContext searchIndexerContext;

    /**
     * Run whilst ensuring the Data Category UID is stored beforehand and removed afterwards.
     */
    @Override
    public void run() {
        try {
            searchIndexer.setDocumentContext(searchIndexerContext);
            searchIndexer.handleDocumentContext();
        } finally {
            CURRENT_CATEGORY_UIDS.remove(searchIndexerContext.dataCategoryUid);
        }
    }

    /**
     * Submit this to the TaskExecutor for execution. This method ensures the same category is not indexed
     * concurrently whilst ensuring later indexing attempts will eventually succeed. Will throw
     * TaskRejectedException if the category is already being indexed.
     *
     * @return true if task is accepted for execution
     */
    public boolean execute() {
        synchronized (CURRENT_CATEGORY_UIDS) {
            // Is the Data Category currently being indexed?
            if (!CURRENT_CATEGORY_UIDS.contains(searchIndexerContext.dataCategoryUid)) {
                // OK to proceed, Data Category is not currently being indexed.
                CURRENT_CATEGORY_UIDS.add(searchIndexerContext.dataCategoryUid);
                taskExecutor.execute(this);
                return true;
            } else {
                // The Data Category was already being indexed so reject this SearchIndexerRunner.
                log.warn("execute() DataCategory is already being indexed: " + searchIndexerContext.dataCategoryUid);
                return false;
            }
        }
    }

    public SearchIndexerContext getSearchIndexerContext() {
        return searchIndexerContext;
    }

    public void setSearchIndexerContext(SearchIndexerContext searchIndexerContext) {
        this.searchIndexerContext = searchIndexerContext;
    }
}
