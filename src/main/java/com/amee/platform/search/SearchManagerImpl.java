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
     * Should the search index be cleared on application start?
     */
    private boolean clearIndex = false;

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
    private Queue<SearchIndexerContext> searchIndexerContextQueue = new ConcurrentLinkedQueue<SearchIndexerContext>();


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
                addDataCategoryContext(ctx);
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
    protected void updateCategories() {
        log.debug("updateCategories()");
        DateTime anHourAgoRoundedUp = new DateTime().minusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        List<DataCategory> dataCategories = dataService.getDataCategoriesModifiedWithin(
                anHourAgoRoundedUp.toDate(),
                anHourAgoRoundedUp.plusHours(1).toDate());
        for (DataCategory dataCategory : dataCategories) {
            SearchIndexerContext ctx = new SearchIndexerContext();
            ctx.dataCategoryUid = dataCategory.getUid();
            ctx.handleDataCategories = indexDataCategories;
            addDataCategoryContext(ctx);
        }
    }

    /**
     * Update all Data Categories & Data Items in the search index where the
     * Data Items have been modified in the last one hour segment.
     */
    protected void updateDataItems() {
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
            addDataCategoryContext(ctx);
        }
    }

    // Index & Document management.

    /**
     * Loops until the application stops (is interrupted). Calls consumeSearchIndexerContextQueue, after
     * a 10 second sleep, to handle any waiting {@link SearchIndexerContext}s.
     */
    public void updateLoop() {
        log.info("updateLoop() Begin.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.debug("updateLoop() Waiting.");
                // Wait 10 seconds before first-run and any subsequent retries.
                Thread.sleep(10 * 1000);
                // Consume the queue.
                consumeSearchIndexerContextQueue();
            } catch (InterruptedException e) {
                log.info("updateLoop() Interrupted.");
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
        // Always make sure index is unlocked.
        luceneService.unlockIndex();
        // Clear the index?
        if (clearIndex) {
            luceneService.clearIndex();
        }
        // Check DataCategories?
        if (checkDataCategories) {
            buildDataCategories();
        }
    }

    /**
     * Add all DataCategories to the index.
     */
    protected void buildDataCategories() {
        log.info("handleDataCategories()");
        Set<String> dataCategoryUids = getDataCategoryUids();
        buildDataCategories(dataCategoryUids);
    }

    /**
     * Get a Set of Data Category UIDs for all.
     *
     * @return Set of Data Category UIDs
     */
    protected Set<String> getDataCategoryUids() {
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
    protected void buildDataCategories(Set<String> dataCategoryUids) {
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
    protected void buildDataCategory(String dataCategoryUid) {
        log.info("buildDataCategory()");
        SearchIndexerContext context = new SearchIndexerContext();
        context.dataCategoryUid = dataCategoryUid;
        context.handleDataCategories = indexDataCategories;
        context.handleDataItems = indexDataItems;
        addDataCategoryContext(context);
    }

    /**
     * Add a {@link SearchIndexerContext} to the queue, but only if there is not an equivalent object already present.
     *
     * @param context {@link SearchIndexerContext} to add to the queue
     */
    protected synchronized void addDataCategoryContext(SearchIndexerContext context) {
        if (context != null) {
            if (!searchIndexerContextQueue.contains(context)) {
                log.debug("addDataCategoryContext() Adding: " + context.dataCategoryUid);
                searchIndexerContextQueue.add(context);
            } else {
                log.debug("addDataCategoryContext() Skipping: " + context.dataCategoryUid);
            }
        }
    }

    // Task submission.

    /**
     * Loops over the searchIndexerContextQueue and sends waiting {@link SearchIndexerContext}s to be
     * processed by {@link SearchIndexer}s. There are no items in the queue this will return.
     */
    protected void consumeSearchIndexerContextQueue() {
        if (!searchIndexerContextQueue.isEmpty()) {
            log.debug("consumeSearchIndexerContextQueue() Consuming.");
            Iterator<SearchIndexerContext> iterator = searchIndexerContextQueue.iterator();
            while (iterator.hasNext()) {
                SearchIndexerContext next = iterator.next();
                if (next != null) {
                    iterator.remove();
                    log.debug("consumeSearchIndexerContextQueue() Removed: " + next.dataCategoryUid);
                    submitForExecution(next);
                }
            }
        } else {
            log.debug("consumeSearchIndexerContextQueue() Nothing to consume.");
        }
    }

    /**
     * Update the DataCategory in the index using a SearchIndexerRunner for the supplied SearchIndexerContext.
     *
     * @param searchIndexerContext a context for the SearchIndexer
     */
    protected void submitForExecution(SearchIndexerContext searchIndexerContext) {
        // Create SearchIndexerRunner.
        SearchIndexerRunner searchIndexerRunner = applicationContext.getBean(SearchIndexerRunner.class);
        searchIndexerRunner.setSearchIndexerContext(searchIndexerContext);
        // Keep attempting to add searchIndexerRunner to Executor.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Attempt to execute the SearchIndexerRunner
                if (!searchIndexerRunner.execute()) {
                    // Failed to execute the SearchIndexerRunner as the Data Category
                    // is already being indexed. Now we add the SearchIndexerContext back
                    // into the queue so it gets another chance to be executed.
                    addDataCategoryContext(searchIndexerContext);
                }
                break;
            } catch (TaskRejectedException e) {
                log.debug("submitForExecution() Sleeping for a short while.");
                try {
                    // Sleep for a while when executor is full.
                    Thread.sleep(500 + RANDOM.nextInt(10000));
                } catch (InterruptedException e1) {
                    // Give up.
                    log.warn("submitForExecution() Caught InterruptedException: " + e1.getMessage());
                    break;
                }
            }
        }
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

    @Value("#{ systemProperties['amee.clearIndex'] }")
    public void setClearIndex(Boolean clearIndex) {
        this.clearIndex = clearIndex;
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
