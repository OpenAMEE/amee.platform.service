package com.amee.platform.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.util.HashSet;
import java.util.Set;

public class SearchIndexerRunner implements Runnable {

    private final Log log = LogFactory.getLog(getClass());

    // A Set of Data Category UIDs that are currently being executed. This is used by the execute method
    // to ensure the same category is not indexed concurrently whilst allowing later indexing attempts
    // to eventually succeed.
    private final static Set<String> CURRENT_CATEGORY_UIDS = new HashSet<String>();

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
            // Tell the SearchIndexer to start indexing the category.
            searchIndexer.handleSearchIndexerContext(searchIndexerContext);
        } finally {
            // Remove the category from list of categories currently being indexed.
            remove();
        }
    }

    /**
     * Submit this to the TaskExecutor for execution. This method ensures the same category is not indexed
     * concurrently whilst ensuring later indexing attempts will eventually succeed.
     *
     * @throws SearchIndexerRunnerException if the queue is full or the category is already being indexed
     */
    public void execute() throws SearchIndexerRunnerException {
        try {
            // Track the category UID.
            add();
            // Execute this.
            taskExecutor.execute(this);
        } catch (TaskRejectedException e) {
            // The queue was full, remove the category and throw a SearchIndexerRunnerException.
            remove();
            throw new SearchIndexerRunnerException(SearchIndexerRunnerException.Reason.FULL);
        }
    }

    private void add() throws SearchIndexerRunnerException {
        synchronized (CURRENT_CATEGORY_UIDS) {
            // Is the Data Category currently being indexed?
            if (CURRENT_CATEGORY_UIDS.contains(searchIndexerContext.dataCategoryUid)) {
                // The Data Category was already being indexed so reject this SearchIndexerRunner.
                log.info("add() DataCategory is already being indexed: " + searchIndexerContext.dataCategoryUid);
                throw new SearchIndexerRunnerException(SearchIndexerRunnerException.Reason.DUPLICATE);
            }
            // OK to proceed, Data Category is not currently being indexed.
            CURRENT_CATEGORY_UIDS.add(searchIndexerContext.dataCategoryUid);
        }
        log.debug("add() " + searchIndexerContext.dataCategoryUid);
    }

    private void remove() {
        synchronized (CURRENT_CATEGORY_UIDS) {
            CURRENT_CATEGORY_UIDS.remove(searchIndexerContext.dataCategoryUid);
        }
        log.debug("remove() " + searchIndexerContext.dataCategoryUid);
        searchIndexer.clear();
        searchIndexer = null;
    }

    public void setSearchIndexerContext(SearchIndexerContext searchIndexerContext) {
        this.searchIndexerContext = searchIndexerContext;
    }
}
