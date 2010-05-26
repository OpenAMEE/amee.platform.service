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
import com.amee.service.path.PathItemService;
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
    private PathItemService pathItemService;

    @Autowired
    private LuceneService luceneService;

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

    public void build() {
        build(true, false);
    }

    public void build(boolean indexDataCategories, boolean indexDataItems) {
        log.info("build() Building...");
        // Add DataCategories?
        if (indexDataCategories) {
            // Ensure we have an empty Lucene index.
            luceneService.clearIndex();
            // Add DataCategories.
            buildDataCategories();
            // Add DataItems?
            if (indexDataItems) {
                // Add DataItems.
                buildDataItems();
            }
        } else {
            // Always make sure index is unlocked.
            luceneService.unlockIndex();
        }
        log.info("build() Building... DONE");
    }

    /**
     * Add all DataCategories to the index.
     */
    protected void buildDataCategories() {
        log.info("buildDataCategories()");
        transactionController.begin(false);
        List<Document> documents = new ArrayList<Document>();
        for (DataCategory dataCategory :
                dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"))) {
            documents.add(getDocument(dataCategory));
        }
        luceneService.addDocuments(documents);
        transactionController.end();
    }

    /**
     * Add all DataItems to the index.
     */
    protected void buildDataItems() {
        log.info("buildDataItems()");
        transactionController.begin(false);
        List<Document> documents;
        for (DataCategory dataCategory :
                dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"))) {
            if (dataCategory.getItemDefinition() != null) {
                log.info("buildDataItems() " + dataCategory.getName());
                documents = new ArrayList<Document>();
                for (DataItem dataItem : dataService.getDataItems(dataCategory)) {
                    documents.add(getDocument(dataItem));
                }
                luceneService.addDocuments(documents);
            }
        }
        transactionController.end();
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
        for (Object key : dataItem.getItemValuesMap().keySet()) {
            String path = (String) key;
            ItemValue itemValue = dataItem.getItemValuesMap().get(path);
            doc.add(new Field(path, itemValue.getValue(), Field.Store.NO, Field.Index.ANALYZED));
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
            entities.put(
                    ObjectType.DC,
                    dataService.getDataCategoryMap(
                            environmentService.getEnvironmentByName("AMEE"),
                            entityIds.get(ObjectType.DC)));
        }
        // Load DataItems.
        if (entityIds.containsKey(ObjectType.DI)) {
            entities.put(
                    ObjectType.DI,
                    dataService.getDataItemMap(
                            environmentService.getEnvironmentByName("AMEE"),
                            entityIds.get(ObjectType.DI)));
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

    // DataCategory Search.

    public List<DataCategory> getDataCategories(DataCategoryFilter filter) {
        // Filter based on an allowed query parameter.
        if (!filter.getQueries().isEmpty()) {
            BooleanQuery query = new BooleanQuery();
            for (Query q : filter.getQueries().values()) {
                query.add(q, BooleanClause.Occur.MUST);
            }
            return getDataCategories(query);
        } else {
            // Just get a simple list of Data Categories.
            return dataService.getDataCategories(environmentService.getEnvironmentByName("AMEE"));
        }
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
        return dataService.getDataItems(environmentService.getEnvironmentByName("AMEE"), dataItemIds);
    }
}
