package com.amee.platform.search;

import com.amee.base.transaction.TransactionController;
import com.amee.domain.AMEEEntity;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemValue;
import com.amee.domain.path.PathItem;
import com.amee.domain.path.PathItemGroup;
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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchService implements ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

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

    @Transactional(readOnly = true)
    private void onInvalidationMessage(InvalidationMessage invalidationMessage) {
        if (!invalidationMessage.isLocal() && invalidationMessage.getObjectType().equals(ObjectType.DC)) {
            log.debug("onInvalidationMessage() Handling InvalidationMessage.");
            DataCategory dataCategory = dataService.getDataCategoryByUid(invalidationMessage.getEntityUid(), true);
            if ((dataCategory != null) && !dataCategory.isTrash()) {
                update(dataCategory);
            } else {
                remove(invalidationMessage.getObjectType(), invalidationMessage.getEntityUid());
            }
        }
    }

    // Index & Document management.

    public void init() {
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
        // Always make sure index is unlocked.
        luceneService.unlockIndex();
    }

    /**
     * Add all DataCategories to the index.
     */
    protected void buildDataCategories() {
        log.info("buildDataCategories()");
        // Wipe all DataCategory Documents from Lucene index.
        remove(ObjectType.DC);
        // Iterate over all DataCategories and add Documents to Lucene index.
        List<Document> documents = new ArrayList<Document>();
        transactionController.begin(false);
        for (DataCategory dataCategory : dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"))) {
            log.info("buildDataCategories() " + dataCategory.toString());
            documents.add(getDocument(dataCategory));
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
        Set<String> dataCategoryUids = new HashSet<String>();
        for (DataCategory dataCategory : dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"))) {
            if (dataCategory.getItemDefinition() != null) {
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

    protected void buildDataItems(String dataCategoryUid) {
        DataCategory dataCategory;
        transactionController.begin(false);
        dataCategory = dataService.getDataCategoryByUid(dataCategoryUid);
        buildDataItems(dataCategory);
        transactionController.end();
    }

    public void buildDataItems(DataCategory dataCategory) {
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
            documents.add(getDocument(dataItem));
        }
        luceneService.addDocuments(documents);
        metadataService.clearMetadatas();
        localeService.clearLocaleNames();
        log.info("buildDataItems() ...done (" + dataCategory.toString() + ").");
    }

    /**
     * Update index with a new Document for the DataCategory.
     * <p/>
     * TODO: Also need to update any 'child' Documents. Look up with the UID.
     *
     * @param dataCategory to update index with
     */
    protected void update(DataCategory dataCategory) {
        log.debug("update() DataCategory: " + dataCategory.getUid());
        Document document = getDocument(dataCategory);
        luceneService.updateDocument(
                new Term("entityUid", dataCategory.getUid()), document, luceneService.getAnalyzer());
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

    protected Document getDocument(DataCategory dataCategory) {
        PathItemGroup pathItemGroup = pathItemService.getPathItemGroup(dataCategory.getEnvironment());
        PathItem pathItem = pathItemGroup.findByUId(dataCategory.getUid());
        Document doc = getDocument((AMEEEntity) dataCategory);
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

    protected Document getDocument(DataItem dataItem) {
        PathItemGroup pathItemGroup = pathItemService.getPathItemGroup(dataItem.getEnvironment());
        PathItem pathItem = pathItemGroup.findByUId(dataItem.getDataCategory().getUid());
        Document doc = getDocument((AMEEEntity) dataItem);
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
            doc.add(new Field(itemValue.getDisplayPath(), itemValue.getValue(), Field.Store.NO, Field.Index.ANALYZED));
        }
        doc.add(new Field("label", dataItem.getLabel(), Field.Store.NO, Field.Index.ANALYZED));
        return doc;
    }

    private Document getDocument(AMEEEntity entity) {
        Document doc = new Document();
        doc.add(new Field("entityType", entity.getObjectType().getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("entityId", entity.getId().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("entityUid", entity.getUid(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        return doc;
    }

    // Entity search.

    public List<AMEEEntity> getEntities(SearchFilter filter) {
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
        List<Document> documents = luceneService.doSearch(query);
        // Collate entityIds against entityTypes.
        Map<ObjectType, Set<Long>> entityIds = new HashMap<ObjectType, Set<Long>>();
        for (Document document : documents) {
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
        for (Document document : documents) {
            entityId = new Long(document.getField("entityId").stringValue());
            entityType = ObjectType.valueOf(document.getField("entityType").stringValue());
            results.add(entities.get(entityType).get(entityId));
        }
        return results;
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

    public List<DataCategory> getDataCategories(DataCategoryFilter filter) {
        List<DataCategory> dataCategories;
        // Filter based on an allowed query parameter.
        if (!filter.getQueries().isEmpty()) {
            BooleanQuery query = new BooleanQuery();
            for (Query q : filter.getQueries().values()) {
                query.add(q, BooleanClause.Occur.MUST);
            }
            dataCategories = getDataCategories(query);
        } else {
            // Just get a simple list of Data Categories.
            dataCategories = dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"));
        }
        // Pre-loading of EntityTags, LocaleNames & DataCategories.
        if (filter.isLoadEntityTags()) {
            tagService.loadEntityTagsForDataCategories(dataCategories);
        }
        if (filter.isLoadMetadatas()) {
            metadataService.loadMetadatasForDataCategories(dataCategories);
        }
        localeService.loadLocaleNamesForDataCategories(dataCategories);
        return dataCategories;
    }

    public List<DataCategory> getDataCategories(String key, String value) {
        Set<Long> dataCategoryIds = new HashSet<Long>();
        for (Document document : luceneService.doSearch(key, value)) {
            dataCategoryIds.add(new Long(document.getField("entityId").stringValue()));
        }
        return dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"), dataCategoryIds);
    }

    public List<DataCategory> getDataCategories(Query query) {
        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term("entityType", ObjectType.DC.getName())), BooleanClause.Occur.MUST);
        q.add(query, BooleanClause.Occur.MUST);
        Set<Long> dataCategoryIds = new HashSet<Long>();
        for (Document document : luceneService.doSearch(q)) {
            dataCategoryIds.add(new Long(document.getField("entityId").stringValue()));
        }
        return dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"), dataCategoryIds);
    }

    // DataItem search.

    public List<DataItem> getDataItems(DataCategory dataCategory, QueryFilter filter) {
        BooleanQuery query = new BooleanQuery();
        for (Query q : filter.getQueries().values()) {
            query.add(q, BooleanClause.Occur.MUST);
        }
        query.add(new TermQuery(new Term("entityType", ObjectType.DI.getName())), BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("categoryUid", dataCategory.getUid())), BooleanClause.Occur.MUST);
        Set<Long> dataItemIds = new HashSet<Long>();
        for (Document document : luceneService.doSearch(query)) {
            dataItemIds.add(new Long(document.getField("entityId").stringValue()));
        }
        // Get the DataItems.
        List<DataItem> dataItems = dataService.getDataItems(environmentService.getEnvironmentByName("AMEE"), dataItemIds, filter.isLoadDataItemValues());
        // Pre-loading of LocaleNames & DataCategories.
        if (filter.isLoadMetadatas()) {
            metadataService.loadMetadatasForDataItems(dataItems);
        }
        localeService.loadLocaleNamesForDataItems(dataItems, filter.isLoadDataItemValues());
        return dataItems;
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
