package com.amee.platform.search;

import com.amee.base.transaction.AMEETransaction;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.service.data.DataService;
import com.amee.service.invalidation.InvalidationMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SearchManagerImpl implements SearchManager, ApplicationContextAware {

    private final Log log = LogFactory.getLog(getClass());

    // Random for sleep times.
    private final static Random RANDOM = new Random();

    @Autowired
    private DataService dataService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private LuceneService luceneService;

    /**
     * Is this instance the master index node? There can be only one!
     */
    private boolean masterIndex = false;

    /**
     * Should all Data Categories be checked on application start?
     */
    private boolean checkDataCategories = false;

    /**
     * Should all Data Categories be re-indexed on application start?
     */
    private boolean indexDataCategories = false;

    /**
     * Should all Data Items be re-indexed on application start?
     */
    private boolean indexDataItems = false;

    /**
     * The path prefix for Data Categories that should be indexed (e.g., '/lca/ecoinvent').
     */
    private String dataCategoryPathPrefix = null;


    /**
     * A {@link Queue} of {@link SearchIndexerContext}s waiting to be sent to a {@link SearchIndexer}. The
     * queue will only contain one {@link SearchIndexerContext} per Data Category.
     */
    private Queue<SearchIndexerContext> queue = new ConcurrentLinkedQueue<SearchIndexerContext>();

    /**
     * A {@link CountDownLatch} for managing the processing loop for the queue.
     * <p/>
     * The initial default latch has a countdown value of zero so it does not wait on first use.
     */
    private CountDownLatch queueLatch = new CountDownLatch(0);

    // Used to obtain SearchIndexer instances.
    private ApplicationContext applicationContext;

    // Events

    @Override
    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void onApplicationEvent(InvalidationMessage invalidationMessage) {
        if (masterIndex &&
                !invalidationMessage.isLocal() &&
                invalidationMessage.getObjectType().equals(ObjectType.DC) &&
                !invalidationMessage.hasOption("dataCategoryIndexed")) {
            log.trace("onApplicationEvent() Handling InvalidationMessage.");
            DataCategory dataCategory = dataService.getDataCategoryByUid(invalidationMessage.getEntityUid(), null);
            if (dataCategory != null) {
                SearchIndexerContext ctx = new SearchIndexerContext();
                ctx.dataCategoryUid = dataCategory.getUid();
                ctx.handleDataCategories = indexDataCategories;
                ctx.handleDataItems = invalidationMessage.hasOption("indexDataItems");
                ctx.checkDataItems = invalidationMessage.hasOption("checkDataItems");
                addSearchIndexerContext(ctx);
            }
        }
    }

    // Scheduled jobs.

    @Override
    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void update() {
        updateCategories();
        updateDataItems();
    }

    /**
     * Update all Data Categories in the search index which have been modified in
     * the last one hour segment.
     */
    private void updateCategories() {
        log.debug("updateCategories()");
        DateTime anHourAgoRoundedUp = new DateTime().minusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        List<DataCategory> dataCategories = dataService.getDataCategoriesModifiedWithin(
                anHourAgoRoundedUp.toDate(),
                anHourAgoRoundedUp.plusHours(1).toDate());
        for (DataCategory dataCategory : dataCategories) {
            SearchIndexerContext ctx = new SearchIndexerContext();
            ctx.dataCategoryUid = dataCategory.getUid();
            ctx.handleDataCategories = indexDataCategories;
            addSearchIndexerContext(ctx);
        }
    }

    /**
     * Update all Data Categories & Data Items in the search index where the
     * Data Items have been modified in the last one hour segment.
     */
    private void updateDataItems() {
        log.debug("updateDataItems()");
        DateTime anHourAgoRoundedUp = new DateTime().minusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        List<DataCategory> dataCategories = dataService.getDataCategoriesForDataItemsModifiedWithin(
                anHourAgoRoundedUp.toDate(),
                anHourAgoRoundedUp.plusHours(1).toDate());
        for (DataCategory dataCategory : dataCategories) {
            SearchIndexerContext ctx = new SearchIndexerContext();
            ctx.dataCategoryUid = dataCategory.getUid();
            ctx.handleDataCategories = indexDataCategories;
            ctx.handleDataItems = true;
            addSearchIndexerContext(ctx);
        }
    }

    // Index & Document management.

    /**
     * Loops until the application stops (is interrupted). Calls consumeQueue, after
     * a 10 second sleep OR the queue latch has been signalled, to handle any waiting {@link SearchIndexerContext}s.
     */
    public void updateLoop() {
        log.info("updateLoop() Begin.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.debug("updateLoop() Waiting.");
                // Wait until:
                //  * 10 seconds have elapsed OR
                //  * the queue latch reaches zero (this thread has been signalled).
                if (queueLatch.await(10, TimeUnit.SECONDS)) {
                    // Consume the queue.
                    consumeQueue();
                }
            } catch (InterruptedException e) {
                log.debug("updateLoop() Interrupted.");
                return;
            } catch (Exception e) {
                log.error("updateLoop() Caught Exception: " + e.getMessage(), e);
            } catch (Throwable t) {
                log.error("updateLoop() Caught Throwable: " + t.getMessage(), t);
            }
        }
        log.info("updateLoop() End.");
    }

    /**
     * Will update or create the whole search index.
     */
    @Override
    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void updateAll() {
        // Clear the SearchIndexer DataCategory count.
        SearchIndexerImpl.resetCount();
        // Prepare the index; unlock it and potential clear it.
        luceneService.prepareIndex();
        // Check DataCategories?
        if (checkDataCategories) {
            buildDataCategories();
        }
    }

    /**
     * Add all DataCategories to the index.
     */
    private void buildDataCategories() {
        log.info("handleDataCategories()");
        Set<String> dataCategoryUids = getDataCategoryUids();
        buildDataCategories(dataCategoryUids);
    }

    /**
     * Get a Set of Data Category UIDs for all.
     *
     * @return Set of Data Category UIDs
     */
    private Set<String> getDataCategoryUids() {
        log.debug("getDataCategoryUids()");
        // Iterate over all DataCategories and gather DataCategory UIDs.
        Set<String> dataCategoryUids = new HashSet<String>();
        for (DataCategory dataCategory : dataService.getDataCategories()) {
            // Don't index Data Categories whose path starts with '/test'.
            // Only index Data Categories whose path starts with dataCategoryPathPrefix (if set).
            if (!dataCategory.getFullPath().startsWith("/test") &&
                    (StringUtils.isBlank(dataCategoryPathPrefix) || dataCategory.getFullPath().startsWith(dataCategoryPathPrefix))) {
                dataCategoryUids.add(dataCategory.getUid());
            }
        }
        return dataCategoryUids;
    }

    /**
     * Add all DataCategories to the index.
     *
     * @param dataCategoryUids UIDs of Data Categories to index.
     */
    private void buildDataCategories(Set<String> dataCategoryUids) {
        log.info("handleDataCategories()");
        for (String uid : dataCategoryUids) {
            buildDataCategory(uid);
        }
    }

    /**
     * Create a {@link SearchIndexerContext} for the supplied Data Category UID and submit this to the queue.
     *
     * @param dataCategoryUid Data Category UID
     */
    private void buildDataCategory(String dataCategoryUid) {
        log.info("buildDataCategory()");
        SearchIndexerContext context = new SearchIndexerContext();
        context.dataCategoryUid = dataCategoryUid;
        context.handleDataCategories = indexDataCategories;
        context.handleDataItems = indexDataItems;
        addSearchIndexerContext(context);
    }

    // Task submission.

    /**
     * Add a {@link SearchIndexerContext} to the queue, but only if there is not an equivalent object already present.
     *
     * @param context {@link SearchIndexerContext} to add to the queue
     */
    private synchronized void addSearchIndexerContext(SearchIndexerContext context) {
        if (context != null) {
            // Never allow equivalent SearchIndexerContexts to exist in the queue.
            if (!queue.contains(context)) {
                log.debug("addDataCategoryContext() Adding: " + context.dataCategoryUid);
                queue.add(context);
                // Signal the queue loop thread to process the queue.
                signalViaQueueLatch();
            } else {
                log.debug("addDataCategoryContext() Skipping: " + context.dataCategoryUid);
            }
        }
    }

    /**
     * Loops over the queue and sends waiting {@link SearchIndexerContext}s to be
     * processed by {@link SearchIndexer}s. There are no items in the queue this will return immediately. The
     * queue latch will always be reset at the end of this method call.
     */
    private void consumeQueue() {
        if (!queue.isEmpty()) {
            log.debug("consumeQueue() Consuming.");
            Iterator<SearchIndexerContext> iterator = queue.iterator();
            while (iterator.hasNext()) {
                SearchIndexerContext next = iterator.next();
                if (next != null) {
                    iterator.remove();
                    log.debug("consumeQueue() Removed: " + next.dataCategoryUid);
                    submitForExecution(next);
                }
            }
        } else {
            log.debug("consumeQueue() Nothing to consume.");
        }
        // Having processed the queue we can reset the queue latch.
        resetQueueLatch();
    }

    /**
     * Update the DataCategory in the index using a SearchIndexerRunner for the supplied SearchIndexerContext.
     *
     * @param searchIndexerContext a context for the SearchIndexer
     */
    private void submitForExecution(SearchIndexerContext searchIndexerContext) {
        // Create SearchIndexerRunner.
        SearchIndexerRunner searchIndexerRunner = applicationContext.getBean(SearchIndexerRunner.class);
        searchIndexerRunner.setSearchIndexerContext(searchIndexerContext);
        // Attempt to execute the SearchIndexerRunner
        try {
            // The SearchIndexerRunner can be rejected if an equivalent SearchIndexerContext is
            // currently being processed.
            if (!searchIndexerRunner.execute()) {
                // Failed to execute the SearchIndexerRunner as the Data Category
                // is already being indexed. Now we add the SearchIndexerContext back
                // into the queue so it gets another chance to be executed.
                addSearchIndexerContext(searchIndexerContext);
            }
        } catch (TaskRejectedException e) {
            log.debug("submitForExecution() Task was rejected: " + searchIndexerContext.dataCategoryUid);
        }
    }

    /**
     * Reset the queue latch with a new {@link CountDownLatch} with a countdown value of 1.
     */
    private synchronized void resetQueueLatch() {
        queueLatch = new CountDownLatch(1);
    }

    /**
     * Signal the queue looping thread via the {@link CountDownLatch}. This will trigger immediate
     * processing of the  queue.
     */
    private synchronized void signalViaQueueLatch() {
        queueLatch.countDown();
    }

    // Properties.

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Value("#{ systemProperties['amee.masterIndex'] }")
    public void setMasterIndex(Boolean masterIndex) {
        this.masterIndex = masterIndex;
    }

    @Value("#{ systemProperties['amee.checkDataCategories'] }")
    public void setCheckDataCategories(Boolean checkDataCategories) {
        this.checkDataCategories = checkDataCategories;
    }

    @Value("#{ systemProperties['amee.indexDataCategories'] }")
    public void setIndexDataCategories(Boolean indexDataCategories) {
        this.indexDataCategories = indexDataCategories;
    }

    @Value("#{ systemProperties['amee.indexDataItems'] }")
    public void setIndexDataItems(Boolean indexDataItems) {
        this.indexDataItems = indexDataItems;
    }

    @Value("#{ systemProperties['amee.dataCategoryPathPrefix'] }")
    public void setDataCategoryPathPrefix(String dataCategoryPathPrefix) {
        this.dataCategoryPathPrefix = dataCategoryPathPrefix;
    }
}
