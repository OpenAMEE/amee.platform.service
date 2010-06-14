package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import com.amee.base.transaction.TransactionController;
import com.amee.domain.AMEEEntity;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemValue;
import com.amee.domain.path.PathItem;
import com.amee.domain.path.PathItemGroup;
import com.amee.platform.science.Amount;
import com.amee.service.data.DataService;
import com.amee.service.environment.EnvironmentService;
import com.amee.service.invalidation.InvalidationMessage;
import com.amee.service.locale.LocaleService;
import com.amee.service.metadata.MetadataService;
import com.amee.service.path.PathItemService;
import com.amee.service.tag.TagService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchService implements ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    public final static DateTimeFormatter DATE_TO_SECOND = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    public final static Analyzer STANDARD_ANALYZER = new StandardAnalyzer(Version.LUCENE_30);
    public final static Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private DataService dataService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private LocaleService localeService;

    @Autowired
    private TagService tagService;

    @Autowired
    private PathItemService pathItemService;

    @Autowired
    private LuceneService luceneService;

    private boolean clearIndex = false;
    private boolean indexDataCategories = false;
    private boolean indexDataItems = false;

    // Events

    public void onApplicationEvent(ApplicationEvent event) {
        if (InvalidationMessage.class.isAssignableFrom(event.getClass())) {
            onInvalidationMessage((InvalidationMessage) event);
        }
    }

    private void onInvalidationMessage(InvalidationMessage invalidationMessage) {
        if (!invalidationMessage.isLocal() && invalidationMessage.getObjectType().equals(ObjectType.DC)) {
            log.debug("onInvalidationMessage() Handling InvalidationMessage.");
            transactionController.begin(false);
            DataCategory dataCategory = dataService.getDataCategoryByUid(invalidationMessage.getEntityUid(), true);
            if (dataCategory != null) {
                updateDataCategory(dataCategory);
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
    public void updateCategories() {
        log.debug("updateCategories()");
        transactionController.begin(false);
        DateTime anHourAgoRoundedUp = new DateTime().minusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        List<DataCategory> dataCategories = dataService.getDataCategoriesModifiedWithin(
                environmentService.getEnvironmentByName("AMEE"),
                anHourAgoRoundedUp.toDate(),
                anHourAgoRoundedUp.plusHours(1).toDate());
        for (DataCategory dataCategory : dataCategories) {
            updateDataCategory(dataCategory);
        }
        transactionController.end();
    }

    /**
     * Update all Data Categories & Data Items in the search index where the
     * Data Items have been modified in the last one hour segment.
     */
    public void updateDataItems() {
        log.debug("updateDataItems()");
        transactionController.begin(false);
        DateTime anHourAgoRoundedUp = new DateTime().minusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        List<DataCategory> dataCategories = dataService.getDataCategoriesForDataItemsModifiedWithin(
                environmentService.getEnvironmentByName("AMEE"),
                anHourAgoRoundedUp.toDate(),
                anHourAgoRoundedUp.plusHours(1).toDate());
        for (DataCategory dataCategory : dataCategories) {
            updateDataCategory(dataCategory, true);
        }
        transactionController.end();
    }

    // Index & Document management.

    /**
     * Will update or create the whole search index.
     */
    public void init() {
        // Always make sure index is unlocked.
        luceneService.unlockIndex();
        // Clear the index?
        if (clearIndex) {
            luceneService.clearIndex();
        }
        // Add DataCategories?
        if (indexDataCategories) {
            buildDataCategories();
        }
        // Add DataItems?
        if (indexDataItems) {
            // Add DataItems.
            buildDataItems();
        }
    }

    /**
     * Add all DataCategories to the index.
     */
    protected void buildDataCategories() {
        log.info("buildDataCategories()");
        // Wipe all DataCategory Documents from Lucene index.
        if (!clearIndex) {
            remove(ObjectType.DC);
        }
        transactionController.begin(false);
        // We need PathItems to exclude test DataCategories.
        PathItem pathItem;
        PathItemGroup pathItemGroup = pathItemService.getPathItemGroup(environmentService.getEnvironmentByName("AMEE"));
        // Iterate over all DataCategories and add Documents to Lucene index.
        List<Document> documents = new ArrayList<Document>();
        for (DataCategory dataCategory : dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"))) {
            log.info("buildDataCategories() " + dataCategory.toString());
            pathItem = pathItemGroup.findByUId(dataCategory.getUid());
            if (pathItem == null || !pathItem.getFullPath().startsWith("/test")) {
                documents.add(getDocumentForDataCategory(dataCategory, pathItem));
            }
        }
        transactionController.end();
        luceneService.addDocuments(documents);
    }

    /**
     * Add all DataItems to the index.
     */
    protected void buildDataItems() {
        log.info("buildDataItems() Starting...");
        transactionController.begin(false);
        // We need PathItems to exclude test DataCategories.
        PathItem pathItem;
        PathItemGroup pathItemGroup = pathItemService.getPathItemGroup(environmentService.getEnvironmentByName("AMEE"));
        // Iterate over all DataCategories and gather DataCategory UIDs. 
        Set<String> dataCategoryUids = new HashSet<String>();
        for (DataCategory dataCategory : dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"))) {
            pathItem = pathItemGroup.findByUId(dataCategory.getUid());
            if ((pathItem != null) &&
                    !pathItem.getFullPath().startsWith("/test") &&
                    (dataCategory.getItemDefinition() != null)) {
                dataCategoryUids.add(dataCategory.getUid());
            }
        }
        transactionController.end();
        buildDataItems(dataCategoryUids);
        log.info("buildDataItems() ...done.");
    }

    /**
     * Add all DataItems to the index.
     *
     * @param dataCategoryUids UIDs of DataCategories to add.
     */
    protected void buildDataItems(Set<String> dataCategoryUids) {
        for (String uid : dataCategoryUids) {
            buildDataItems(uid);
        }
    }

    /**
     * Create all DataItem documents for the supplied DataCategory UIDs.
     *
     * @param dataCategoryUid DataCategory UIDs to create DataItem documents for
     */
    protected void buildDataItems(String dataCategoryUid) {
        DataCategory dataCategory;
        transactionController.begin(false);
        dataCategory = dataService.getDataCategoryByUid(dataCategoryUid);
        buildDataItems(dataCategory);
        transactionController.end();
    }

    /**
     * Create all DataItem documents for the supplied DataCategory.
     *
     * @param dataCategory DataCategory to create DataItem documents for
     */
    public void buildDataItems(DataCategory dataCategory) {
        if (dataCategory.getItemDefinition() != null) {
            log.info("buildDataItems() Starting... (" + dataCategory.toString() + ")");
            metadataService.loadMetadatasForItemValueDefinitions(dataCategory.getItemDefinition().getItemValueDefinitions());
            localeService.loadLocaleNamesForItemValueDefinitions(dataCategory.getItemDefinition().getItemValueDefinitions());
            List<DataItem> dataItems = dataService.getDataItems(dataCategory, false);
            metadataService.loadMetadatasForDataItems(dataItems);
            localeService.loadLocaleNamesForDataItems(dataItems);
            // Clear existing DataItem Documents?
            if (!clearIndex) {
                removeDataItems(dataCategory);
            }
            // Create DataItem Documents and store to Lucene.
            List<Document> documents = new ArrayList<Document>();
            for (DataItem dataItem : dataItems) {
                documents.add(getDocumentForDataItem(dataItem));
            }
            luceneService.addDocuments(documents);
            metadataService.clearMetadatas();
            localeService.clearLocaleNames();
            log.info("buildDataItems() ...done (" + dataCategory.toString() + ").");
        } else {
            log.debug("buildDataItems() DataCategory does not have items: " + dataCategory.toString());
        }
    }

    /**
     * Update or remove a Data Category from the search index.
     *
     * @param dataCategory to update index with
     */
    protected void updateDataCategory(DataCategory dataCategory) {
        updateDataCategory(dataCategory, false);
    }

    /**
     * Update or remove Data Category & Data Items from the search index.
     *
     * @param dataCategory to update index with
     * @param dataItems    true if Data Items should also be updated
     */
    protected void updateDataCategory(DataCategory dataCategory, boolean dataItems) {
        log.debug("updateDataCategory() DataCategory: " + dataCategory.toString());
        if (!dataCategory.isTrash()) {
            Document document = getDocument(dataCategory);
            if (document != null) {
                Field modifiedField = document.getField("entityModified");
                if (modifiedField != null) {
                    DateTime modifiedInIndex =
                            DATE_TO_SECOND.parseDateTime(modifiedField.stringValue());
                    DateTime modifiedInDatabase =
                            new DateTime(dataCategory.getModified()).withMillisOfSecond(0);
                    if (modifiedInDatabase.isAfter(modifiedInIndex)) {
                        log.debug("updateDataCategory() DataCategory has been modified, updating.");
                        addDataCategory(dataCategory);
                    } else {
                        log.debug("updateDataCategory() DataCategory is up-to-date, skipping.");
                    }
                } else {
                    log.warn("updateDataCategory() The modified field was missing, updating");
                    addDataCategory(dataCategory);
                }
                // Should we also update the DataItems now?
                if (dataItems) {
                    buildDataItems(dataCategory);
                }
            } else {
                log.debug("updateDataCategory() DataCategory not in index, adding.");
                addDataCategory(dataCategory);
                buildDataItems(dataCategory);
            }
        } else {
            log.debug("updateDataCategory() DataCategory needs to be removed.");
            removeDataCategory(dataCategory);
            removeDataItems(dataCategory);
        }
    }

    /**
     * Add a Document for the supplied DataCategory to the Lucene index.
     *
     * @param dataCategory to add to the search index.
     */
    protected void addDataCategory(DataCategory dataCategory) {
        log.debug("addDataCategory() " + dataCategory.toString());
        PathItemGroup pathItemGroup = pathItemService.getPathItemGroup(environmentService.getEnvironmentByName("AMEE"));
        Document document = getDocumentForDataCategory(dataCategory, pathItemGroup.findByUId(dataCategory.getUid()));
        luceneService.updateDocument(
                document,
                new Term("entityType", ObjectType.DC.getName()),
                new Term("entityUid", dataCategory.getUid()));
    }

    /**
     * Removes a document from the index.
     *
     * @param entityType of document to remove
     * @param uid        of document to remove
     */
    protected void remove(ObjectType entityType, String uid) {
        log.debug("remove() " + uid);
        luceneService.deleteDocuments(
                new Term("entityType", entityType.getName()),
                new Term("entityUid", uid));
    }

    /**
     * Removes documents from the index.
     *
     * @param entityType of document to remove
     */
    protected void remove(ObjectType entityType) {
        log.debug("remove() " + entityType.getName());
        luceneService.deleteDocuments(new Term("entityType", entityType.getName()));
    }

    /**
     * Remove the supplied DataCategory from the search index.
     *
     * @param dataCategory to remove
     */
    protected void removeDataCategory(DataCategory dataCategory) {
        log.debug("removeDataCatgory() " + dataCategory.toString());
        remove(ObjectType.DC, dataCategory.getUid());
    }

    /**
     * Removes all DataItem Documents from the index for a DataCategory.
     *
     * @param dataCategory to remove Data Items Documents for
     */
    protected void removeDataItems(DataCategory dataCategory) {
        log.debug("removeDataItems() " + dataCategory.toString());
        luceneService.deleteDocuments(
                new Term("entityType", ObjectType.DI.getName()),
                new Term("categoryUid", dataCategory.getUid()));
    }

    protected Document getDocumentForDataCategory(DataCategory dataCategory, PathItem pathItem) {
        Document doc = getDocumentForAMEEEntity(dataCategory);
        doc.add(new Field("name", dataCategory.getName(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("path", dataCategory.getPath(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        if (pathItem != null) {
            doc.add(new Field("fullPath", pathItem.getFullPath(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        }
        doc.add(new Field("wikiName", dataCategory.getWikiName(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("wikiDoc", dataCategory.getWikiDoc(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("provenance", dataCategory.getProvenance(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("authority", dataCategory.getAuthority(), Field.Store.NO, Field.Index.ANALYZED));
        if (dataCategory.getDataCategory() != null) {
            doc.add(new Field("parentUid", dataCategory.getDataCategory().getUid(), Field.Store.NO, Field.Index.NOT_ANALYZED));
            doc.add(new Field("parentWikiName", dataCategory.getDataCategory().getWikiName(), Field.Store.NO, Field.Index.ANALYZED));
        }
        if (dataCategory.getItemDefinition() != null) {
            doc.add(new Field("itemDefinitionUid", dataCategory.getItemDefinition().getUid(), Field.Store.NO, Field.Index.NOT_ANALYZED));
            doc.add(new Field("itemDefinitionName", dataCategory.getItemDefinition().getName(), Field.Store.NO, Field.Index.ANALYZED));
        }
        doc.add(new Field("tags", tagService.getTagsCSV(dataCategory), Field.Store.NO, Field.Index.ANALYZED));
        return doc;
    }

    protected Document getDocumentForDataItem(DataItem dataItem) {
        PathItemGroup pathItemGroup = pathItemService.getPathItemGroup(dataItem.getEnvironment());
        PathItem pathItem = pathItemGroup.findByUId(dataItem.getDataCategory().getUid());
        Document doc = getDocumentForAMEEEntity(dataItem);
        doc.add(new Field("name", dataItem.getName(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("path", dataItem.getPath(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        if (pathItem != null) {
            doc.add(new Field("fullPath", pathItem.getFullPath() + "/" + dataItem.getDisplayPath(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        }
        doc.add(new Field("wikiDoc", dataItem.getWikiDoc(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("provenance", dataItem.getProvenance(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("categoryUid", dataItem.getDataCategory().getUid(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("categoryWikiName", dataItem.getDataCategory().getWikiName(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("itemDefinitionUid", dataItem.getItemDefinition().getUid(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("itemDefinitionName", dataItem.getItemDefinition().getName(), Field.Store.NO, Field.Index.ANALYZED));
        for (ItemValue itemValue : dataItem.getItemValues()) {
            if (itemValue.isUsableValue()) {
                if (itemValue.getItemValueDefinition().isDrillDown()) {
                    doc.add(new Field(itemValue.getDisplayPath(), itemValue.getValue(), Field.Store.NO, Field.Index.NOT_ANALYZED));
                } else {
                    if (itemValue.isDouble()) {
                        try {
                            doc.add(new NumericField(itemValue.getDisplayPath()).setDoubleValue(new Amount(itemValue.getValue()).getValue()));
                        } catch (NumberFormatException e) {
                            log.warn("getDocumentForDataItem() Could not parse '" + itemValue.getDisplayPath() + "' value '" + itemValue.getValue() + "' for DataItem " + dataItem.toString() + ".");
                            doc.add(new Field(itemValue.getDisplayPath(), itemValue.getValue(), Field.Store.NO, Field.Index.ANALYZED));
                        }
                    } else {
                        doc.add(new Field(itemValue.getDisplayPath(), itemValue.getValue(), Field.Store.NO, Field.Index.ANALYZED));
                    }
                }
            }
        }
        doc.add(new Field("label", dataItem.getLabel(), Field.Store.NO, Field.Index.ANALYZED));
        return doc;
    }

    private Document getDocumentForAMEEEntity(AMEEEntity entity) {
        Document doc = new Document();
        doc.add(new Field("entityType", entity.getObjectType().getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("entityId", entity.getId().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("entityUid", entity.getUid(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("entityCreated",
                new DateTime(entity.getCreated()).toString(DATE_TO_SECOND), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("entityModified",
                new DateTime(entity.getModified()).toString(DATE_TO_SECOND), Field.Store.YES, Field.Index.NOT_ANALYZED));
        return doc;
    }

    // Entity search.

    public ResultsWrapper<AMEEEntity> getEntities(SearchFilter filter) {
        Query query;
        Long entityId;
        ObjectType entityType;
        // Obtain Query - Do we need to respect types?
        if (!filter.getTypes().isEmpty()) {
            // Only search specified types.
            // First - add entityType queries.
            BooleanQuery typesQuery = new BooleanQuery();
            for (ObjectType objectType : filter.getTypes()) {
                typesQuery.add(new TermQuery(new Term("entityType", objectType.getName())), BooleanClause.Occur.SHOULD);
            }
            // Second - combine filter query and types query
            BooleanQuery combinedQuery = new BooleanQuery();
            combinedQuery.add(typesQuery, BooleanClause.Occur.MUST);
            combinedQuery.add(filter.getQ(), BooleanClause.Occur.MUST);
            query = combinedQuery;
        } else {
            // Search all entities with filter query.
            query = filter.getQ();
        }
        // Do search and fetch Lucene documents.
        ResultsWrapper<Document> resultsWrapper = luceneService.doSearch(query, filter.getResultStart(), filter.getResultLimit());
        // Collate entityIds against entityTypes.
        Map<ObjectType, Set<Long>> entityIds = new HashMap<ObjectType, Set<Long>>();
        for (Document document : resultsWrapper.getResults()) {
            entityId = new Long(document.getField("entityId").stringValue());
            entityType = ObjectType.valueOf(document.getField("entityType").stringValue());
            Set<Long> idSet = entityIds.get(entityType);
            if (idSet == null) {
                idSet = new HashSet<Long>();
                entityIds.put(entityType, idSet);
            }
            idSet.add(entityId);
        }
        // Collate AMEEEntities.
        Map<ObjectType, Map<Long, AMEEEntity>> entities = new HashMap<ObjectType, Map<Long, AMEEEntity>>();
        // Load DataCategories.
        if (entityIds.containsKey(ObjectType.DC)) {
            Map<Long, DataCategory> dataCategoriesMap = dataService.getDataCategoryMap(
                    environmentService.getEnvironmentByName("AMEE"),
                    entityIds.get(ObjectType.DC));
            addDataCategories(entities, dataCategoriesMap);
            // Pre-loading of EntityTags, LocaleNames & DataCategories.
            if (filter.isLoadEntityTags()) {
                tagService.loadEntityTagsForDataCategories(dataCategoriesMap.values());
            }
            if (filter.isLoadMetadatas()) {
                metadataService.loadMetadatasForDataCategories(dataCategoriesMap.values());
            }
            localeService.loadLocaleNamesForDataCategories(dataCategoriesMap.values());
        }
        // Load DataItems.
        if (entityIds.containsKey(ObjectType.DI)) {
            Map<Long, DataItem> dataItemsMap = dataService.getDataItemMap(
                    environmentService.getEnvironmentByName("AMEE"),
                    entityIds.get(ObjectType.DI),
                    filter.isLoadDataItemValues());
            addDataItems(entities, dataItemsMap);
            // Pre-loading of LocaleNames & DataCategories.
            if (filter.isLoadMetadatas()) {
                metadataService.loadMetadatasForDataItems(dataItemsMap.values());
            }
            localeService.loadLocaleNamesForDataItems(dataItemsMap.values(), filter.isLoadDataItemValues());
        }
        // Create result list in relevance order.
        List<AMEEEntity> results = new ArrayList<AMEEEntity>();
        for (Document document : resultsWrapper.getResults()) {
            entityId = new Long(document.getField("entityId").stringValue());
            entityType = ObjectType.valueOf(document.getField("entityType").stringValue());
            results.add(entities.get(entityType).get(entityId));
        }
        return new ResultsWrapper<AMEEEntity>(results, resultsWrapper.isTruncated());
    }

    protected void addDataCategories(Map<ObjectType, Map<Long, AMEEEntity>> entities, Map<Long, DataCategory> dataCategoriesMap) {
        Map<Long, AMEEEntity> e = new HashMap<Long, AMEEEntity>();
        for (Long id : dataCategoriesMap.keySet()) {
            e.put(id, dataCategoriesMap.get(id));
        }
        entities.put(ObjectType.DC, e);
    }

    protected void addDataItems(Map<ObjectType, Map<Long, AMEEEntity>> entities, Map<Long, DataItem> dataItemsMap) {
        Map<Long, AMEEEntity> e = new HashMap<Long, AMEEEntity>();
        for (Long id : dataItemsMap.keySet()) {
            e.put(id, dataItemsMap.get(id));
        }
        entities.put(ObjectType.DI, e);
    }

    // DataCategory Search.

    public ResultsWrapper<DataCategory> getDataCategories(DataCategoryFilter filter) {
        ResultsWrapper<DataCategory> resultsWrapper;
        // Filter based on an allowed query parameter.
        if (!filter.getQueries().isEmpty()) {
            BooleanQuery query = new BooleanQuery();
            for (Query q : filter.getQueries().values()) {
                query.add(q, BooleanClause.Occur.MUST);
            }
            resultsWrapper = getDataCategories(
                    query,
                    filter.getResultStart(),
                    filter.getResultLimit());
        } else {
            // Just get a simple list of Data Categories.
            resultsWrapper = dataService.getDataCategories(
                    environmentService.getEnvironmentByName("AMEE"),
                    filter.getResultStart(),
                    filter.getResultLimit());
        }
        // Pre-loading of EntityTags, LocaleNames & DataCategories.
        if (filter.isLoadEntityTags()) {
            tagService.loadEntityTagsForDataCategories(resultsWrapper.getResults());
        }
        if (filter.isLoadMetadatas()) {
            metadataService.loadMetadatasForDataCategories(resultsWrapper.getResults());
        }
        localeService.loadLocaleNamesForDataCategories(resultsWrapper.getResults());
        return resultsWrapper;
    }

    public ResultsWrapper<DataCategory> getDataCategories(Query query, int resultStart, int resultLimit) {
        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term("entityType", ObjectType.DC.getName())), BooleanClause.Occur.MUST);
        q.add(query, BooleanClause.Occur.MUST);
        ResultsWrapper<Document> resultsWrapper =
                luceneService.doSearch(q, resultStart, resultLimit);
        Set<Long> dataCategoryIds = new HashSet<Long>();
        for (Document document : resultsWrapper.getResults()) {
            dataCategoryIds.add(new Long(document.getField("entityId").stringValue()));
        }
        return new ResultsWrapper<DataCategory>(
                dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"), dataCategoryIds),
                resultsWrapper.isTruncated());
    }

    // DataItem search.

    public ResultsWrapper<DataItem> getDataItems(DataCategory dataCategory, DataItemFilter filter) {
        BooleanQuery query = new BooleanQuery();
        for (Query q : filter.getQueries().values()) {
            query.add(q, BooleanClause.Occur.MUST);
        }
        // Get the DataItems.
        ResultsWrapper<DataItem> resultsWrapper = getDataItems(dataCategory, query, filter.getResultStart(), filter.getResultLimit());
        // Pre-loading of Metadatas, LocaleNames and ItemValues.
        if (filter.isLoadMetadatas()) {
            metadataService.loadMetadatasForDataItems(resultsWrapper.getResults());
        }
        localeService.loadLocaleNamesForDataItems(resultsWrapper.getResults(), filter.isLoadDataItemValues());
        return resultsWrapper;
    }

    public ResultsWrapper<DataItem> getDataItems(DataCategory dataCategory, Query query, int resultStart, int resultLimit) {
        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term("entityType", ObjectType.DI.getName())), BooleanClause.Occur.MUST);
        q.add(new TermQuery(new Term("categoryUid", dataCategory.getUid())), BooleanClause.Occur.MUST);
        q.add(query, BooleanClause.Occur.MUST);
        ResultsWrapper<Document> resultsWrapper =
                luceneService.doSearch(q, resultStart, resultLimit);
        Set<Long> dataCategoryIds = new HashSet<Long>();
        for (Document document : resultsWrapper.getResults()) {
            dataCategoryIds.add(new Long(document.getField("entityId").stringValue()));
        }
        return new ResultsWrapper<DataItem>(
                dataService.getDataItems(environmentService.getEnvironmentByName("AMEE"), dataCategoryIds),
                resultsWrapper.isTruncated());
    }

    // Document search.

    /**
     * Find a single Lucene Document that matches the supplied entity.
     *
     * @param entity to search for
     * @return Document matching entity or null
     */
    protected Document getDocument(AMEEEntity entity) {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("entityType", entity.getObjectType().getName())), BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("entityUid", entity.getUid())), BooleanClause.Occur.MUST);
        ResultsWrapper<Document> resultsWrapper = luceneService.doSearch(query, 0, 1);
        if (resultsWrapper.isTruncated()) {
            log.warn("getDocument() Entity in index more than once: " + entity.toString());
        }
        if (!resultsWrapper.getResults().isEmpty()) {
            return resultsWrapper.getResults().get(0);
        } else {
            return null;
        }
    }

    @Value("#{ systemProperties['amee.clearIndex'] }")
    public void setClearIndex(Boolean clearIndex) {
        this.clearIndex = clearIndex;
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
