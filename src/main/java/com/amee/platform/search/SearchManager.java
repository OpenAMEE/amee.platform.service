package com.amee.platform.search;

import com.amee.base.transaction.TransactionController;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.service.data.DataService;
import com.amee.service.invalidation.InvalidationMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SearchManager implements Runnable, SmartLifecycle, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    // Random for sleep times.
    private final static Random RANDOM = new Random();

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private DataService dataService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private LuceneService luceneService;

    @Autowired
    @Qualifier("searchIndexerTaskExecutor")
    private TaskExecutor taskExecutor;

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

    // A Thread to do the initialisation work in.
    private Thread thread;

    // A flag to indicate the thread should stop soon.
    private boolean stopping = false;

    // Application start-up initialisation.

    @Override
    public synchronized void start() {
        log.info("start()");
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public synchronized void stop() {
        log.info("stop()");
        stopping = true;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public boolean isRunning() {
        return (thread != null) && (thread.isAlive());
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        // Start as late as possible.
        return Integer.MAX_VALUE;
    }

    @Override
    public void run() {
        log.info("run() Starting...");
        init();
        log.info("run() ...done.");
    }

    // Events

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (InvalidationMessage.class.isAssignableFrom(event.getClass())) {
            onInvalidationMessage((InvalidationMessage) event);
        }
    }

    protected void onInvalidationMessage(InvalidationMessage invalidationMessage) {
        if (masterIndex &&
                !invalidationMessage.isLocal() &&
                invalidationMessage.getObjectType().equals(ObjectType.DC) &&
                !invalidationMessage.hasOption("dataCategoryIndexed")) {
            log.debug("onInvalidationMessage() Handling InvalidationMessage.");
            transactionController.begin(false);
            DataCategory dataCategory = dataService.getDataCategoryByUid(invalidationMessage.getEntityUid(), null);
            if (dataCategory != null) {
                SearchIndexer.DocumentContext ctx = new SearchIndexer.DocumentContext();
                ctx.dataCategoryUid = dataCategory.getUid();
                ctx.handleDataCategories = indexDataCategories;
                ctx.handleDataItems = invalidationMessage.hasOption("indexDataItems");
                updateDataCategory(ctx);
            }
            transactionController.end();
        }
    }

    // Scheduled jobs.

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
        transactionController.begin(false);
        DateTime anHourAgoRoundedUp = new DateTime().minusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        List<DataCategory> dataCategories = dataService.getDataCategoriesModifiedWithin(
                anHourAgoRoundedUp.toDate(),
                anHourAgoRoundedUp.plusHours(1).toDate());
        for (DataCategory dataCategory : dataCategories) {
            SearchIndexer.DocumentContext ctx = new SearchIndexer.DocumentContext();
            ctx.dataCategoryUid = dataCategory.getUid();
            ctx.handleDataCategories = indexDataCategories;
            updateDataCategory(ctx);
        }
        transactionController.end();
    }

    /**
     * Update all Data Categories & Data Items in the search index where the
     * Data Items have been modified in the last one hour segment.
     */
    protected void updateDataItems() {
        log.debug("updateDataItems()");
        transactionController.begin(false);
        DateTime anHourAgoRoundedUp = new DateTime().minusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        List<DataCategory> dataCategories = dataService.getDataCategoriesForDataItemsModifiedWithin(
                anHourAgoRoundedUp.toDate(),
                anHourAgoRoundedUp.plusHours(1).toDate());
        for (DataCategory dataCategory : dataCategories) {
            SearchIndexer.DocumentContext ctx = new SearchIndexer.DocumentContext();
            ctx.dataCategoryUid = dataCategory.getUid();
            ctx.handleDataCategories = indexDataCategories;
            ctx.handleDataItems = true;
            updateDataCategory(ctx);
        }
        transactionController.end();
    }

    // Index & Document management.

    /**
     * Will update or create the whole search index.
     */
    public void init() {
        // Clear the SearchIndexer DataCategory count.
        SearchIndexer.resetCount();
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
        buildDataCategories(getDataCategoryUids());
    }

    /**
     * Get a Set of Data Category UIDs for all.
     *
     * @return Set of Data Category UIDs
     */
    protected Set<String> getDataCategoryUids() {
        log.debug("getDataCategoryUids()");
        transactionController.begin(false);
        // Iterate over all DataCategories and gather DataCategory UIDs.
        Set<String> dataCategoryUids = new HashSet<String>();
        for (DataCategory dataCategory : dataService.getDataCategories()) {
            if (!dataCategory.getFullPath().startsWith("/test")) {
                dataCategoryUids.add(dataCategory.getUid());
            }
        }
        transactionController.end();
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

    protected void buildDataCategory(String dataCategoryUid) {
        log.info("buildDataCategory()");
        transactionController.begin(false);
        SearchIndexer.DocumentContext ctx = new SearchIndexer.DocumentContext();
        ctx.dataCategoryUid = dataCategoryUid;
        ctx.handleDataCategories = indexDataCategories;
        ctx.handleDataItems = indexDataItems;
        updateDataCategory(ctx);
        transactionController.end();
    }

    // Task submission.

    protected void updateDataCategory(SearchIndexer.DocumentContext ctx) {
        // Create LcaImporter.
        SearchIndexer searchIndexer = new SearchIndexer(ctx);
        // Keep attempting to add LcaImporter to Executor.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                taskExecutor.execute(searchIndexer);
                break;
            } catch (TaskRejectedException e) {
                log.info("updateDataCategory() Sleeping for a short while.");
                try {
                    // Sleep for a while when executor is full.
                    Thread.sleep(500 + RANDOM.nextInt(10000));
                } catch (InterruptedException e1) {
                    // Give up.
                    log.warn("updateDataCategory() Caught InterruptedException: " + e1.getMessage());
                    break;
                }
            }
        }
    }

    // Properties.

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
}
