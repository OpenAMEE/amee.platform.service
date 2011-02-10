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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    /**
     * The path prefix for Data Categories that should be indexed (e.g., '/lca/ecoinvent').
     */
    private String dataCategoryPathPrefix = null;

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
            log.debug("onApplicationEvent() Handling InvalidationMessage.");
            DataCategory dataCategory = dataService.getDataCategoryByUid(invalidationMessage.getEntityUid(), null);
            if (dataCategory != null) {
                SearchIndexerContext ctx = new SearchIndexerContext();
                ctx.dataCategoryUid = dataCategory.getUid();
                ctx.handleDataCategories = indexDataCategories;
                ctx.handleDataItems = invalidationMessage.hasOption("indexDataItems");
                updateDataCategory(ctx);
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
            updateDataCategory(ctx);
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
            updateDataCategory(ctx);
        }
    }

    // Index & Document management.

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

    protected void buildDataCategory(String dataCategoryUid) {
        log.info("buildDataCategory()");
        SearchIndexerContext ctx = new SearchIndexerContext();
        ctx.dataCategoryUid = dataCategoryUid;
        ctx.handleDataCategories = indexDataCategories;
        ctx.handleDataItems = indexDataItems;
        updateDataCategory(ctx);
    }

    // Task submission.

    protected void updateDataCategory(SearchIndexerContext searchIndexerContext) {
        // Create SearchIndexerRunner.
        SearchIndexerRunner searchIndexerRunner = applicationContext.getBean(SearchIndexerRunner.class);
        searchIndexerRunner.setDocumentContext(searchIndexerContext);
        // Keep attempting to add searchIndexerRunner to Executor.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                taskExecutor.execute(searchIndexerRunner);
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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

    @Value("#{ systemProperties['amee.dataCategoryPathPrefix'] }")
    public void setDataCategoryPathPrefix(String dataCategoryPathPrefix) {
        this.dataCategoryPathPrefix = dataCategoryPathPrefix;
    }
}
