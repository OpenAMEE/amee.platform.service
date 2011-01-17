package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.IAMEEEntity;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.item.data.NuDataItem;
import com.amee.service.data.DataService;
import com.amee.service.item.DataItemService;
import com.amee.service.locale.LocaleService;
import com.amee.service.metadata.MetadataService;
import com.amee.service.tag.TagService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Reader;
import java.util.*;

public class SearchService {

    private final Log log = LogFactory.getLog(getClass());

    public final static Analyzer STANDARD_ANALYZER = new StandardAnalyzer(Version.LUCENE_30);
    public final static Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();
    public final static Analyzer LOWER_CASE_KEYWORD_ANALYZER = new Analyzer() {
        public TokenStream tokenStream(String fieldName, Reader reader) {
            TokenStream result = new KeywordTokenizer(reader);
            result = new LowerCaseFilter(result);
            return result;
        }
    };

    @Autowired
    private DataService dataService;

    @Autowired
    private DataItemService dataItemService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private LocaleService localeService;

    @Autowired
    private TagService tagService;

    @Autowired
    private SearchQueryService searchQueryService;

    @Autowired
    private LuceneService luceneService;

    // Entity search.

    public ResultsWrapper<IAMEEEntity> getEntities(SearchFilter filter) {
        // Ensure filter.types contains NDI if DI is present.
        if (filter.getTypes().contains(ObjectType.DI)) {
            filter.getTypes().add(ObjectType.NDI);
        }
        // Do search and fetch Lucene documents.
        return getEntityResultsWrapper(
                searchQueryService.doSearch(filter),
                filter.isLoadEntityTags(),
                filter.isLoadMetadatas(),
                filter.isLoadDataItemValues());
    }

    protected void addDataCategories(Map<ObjectType, Map<String, IAMEEEntity>> entities, Map<String, DataCategory> dataCategoriesMap) {
        Map<String, IAMEEEntity> e = new HashMap<String, IAMEEEntity>();
        for (String uid : dataCategoriesMap.keySet()) {
            e.put(uid, dataCategoriesMap.get(uid));
        }
        entities.put(ObjectType.DC, e);
    }

    protected void addDataItems(Map<ObjectType, Map<String, IAMEEEntity>> entities, Map<String, DataItem> dataItemsMap) {
        Map<String, IAMEEEntity> dataItems = new HashMap<String, IAMEEEntity>();
        for (String id : dataItemsMap.keySet()) {
            IAMEEEntity entity = dataItemsMap.get(id);
            if (entity.getObjectType().equals(ObjectType.DI)) {
                dataItems.put(id, dataItemsMap.get(id));
            } else {
                throw new IllegalStateException("An ObjectType of DI was expected.");
            }
        }
        entities.put(ObjectType.DI, dataItems);
    }

    protected void addNuDataItems(Map<ObjectType, Map<String, IAMEEEntity>> entities, Map<String, NuDataItem> dataItemsMap) {
        Map<String, IAMEEEntity> dataItems = new HashMap<String, IAMEEEntity>();
        for (String uid : dataItemsMap.keySet()) {
            IAMEEEntity entity = dataItemsMap.get(uid);
            if (entity.getObjectType().equals(ObjectType.NDI)) {
                dataItems.put(uid, DataItem.getDataItem(dataItemsMap.get(uid)));
            } else {
                throw new IllegalStateException("An ObjectType of NDI was expected.");
            }
        }
        entities.put(ObjectType.NDI, dataItems);
    }

    // DataCategory Search.

    // TODO: Sort the results
    public ResultsWrapper<DataCategory> getDataCategories(DataCategoriesFilter filter) {
        ResultsWrapper<DataCategory> resultsWrapper;
        // Filter based on an allowed query parameter.
        if (!filter.getQueries().isEmpty()) {
            BooleanQuery query = new BooleanQuery();
            Query excTags = filter.removeExcTags();
            if (excTags != null) {
                query.add(excTags, BooleanClause.Occur.MUST_NOT);
            }
            for (Query q : filter.getQueries().values()) {
                query.add(q, BooleanClause.Occur.MUST);
            }
            resultsWrapper = getDataCategories(query, filter);
        } else {
            // Just get a simple list of Data Categories.
            resultsWrapper = dataService.getDataCategories(
                    true,
                    filter.getResultStart(),
                    filter.getResultLimit());
            // Pre-loading of EntityTags, LocaleNames & DataCategories.
            if (filter.isLoadEntityTags()) {
                tagService.loadEntityTagsForDataCategories(resultsWrapper.getResults());
            }
            if (filter.isLoadMetadatas()) {
                metadataService.loadMetadatasForDataCategories(resultsWrapper.getResults());
            }
        }
        return resultsWrapper;
    }

    // TODO: Write an integration test for these sorted results
    private ResultsWrapper<DataCategory> getDataCategories(BooleanQuery query, DataCategoriesFilter filter) {
        if (query == null) {
            query = new BooleanQuery();
        }
        Query entityQuery = new TermQuery(new Term("entityType", ObjectType.DC.getName()));
        query.add(entityQuery, BooleanClause.Occur.MUST);
        return getDataCategoryResultsWrapper(
                getEntityResultsWrapper(
                        luceneService.doSearch(
                                query,
                                filter.getResultStart(),
                                filter.getResultLimit(), LuceneServiceImpl.MAX_NUM_HITS, "byWikiName"),
                        filter.isLoadEntityTags(),
                        filter.isLoadMetadatas(),
                        false));
    }

    // TODO: Find a way to genericise this and the similar method below for Data Items.
    private ResultsWrapper<DataCategory> getDataCategoryResultsWrapper(ResultsWrapper<IAMEEEntity> entityResultsWrapper) {
        ResultsWrapper<DataCategory> dataCategoryResultsWrapper = new ResultsWrapper<DataCategory>();
        dataCategoryResultsWrapper.setTruncated(entityResultsWrapper.isTruncated());
        dataCategoryResultsWrapper.setResultStart(entityResultsWrapper.getResultStart());
        dataCategoryResultsWrapper.setResultLimit(entityResultsWrapper.getResultLimit());
        dataCategoryResultsWrapper.setHits(entityResultsWrapper.getHits());
        List<DataCategory> dataCategories = new ArrayList<DataCategory>();
        for (IAMEEEntity entity : entityResultsWrapper.getResults()) {
            if (DataCategory.class.isAssignableFrom(entity.getClass())) {
                dataCategories.add((DataCategory) entity);
            } else {
                throw new IllegalStateException("A DataCategory was expected.");
            }
        }
        dataCategoryResultsWrapper.setResults(dataCategories);
        return dataCategoryResultsWrapper;
    }

    // DataItem search.

    public ResultsWrapper<DataItem> getDataItems(DataCategory dataCategory, DataItemsFilter filter) {
        // Create Query.
        BooleanQuery query = null;
        if (!filter.getQueries().isEmpty()) {
            query = new BooleanQuery();
            for (Query q : filter.getQueries().values()) {
                query.add(q, BooleanClause.Occur.MUST);
            }
        }
        // Get the DataItems.
        return getDataItems(dataCategory, filter, query);
    }

    private ResultsWrapper<DataItem> getDataItems(DataCategory dataCategory, DataItemsFilter filter, Query query) {
        // Create Query to find all DIs & NDIs in the DataCategory matching the supplied Query and range.
        BooleanQuery q = new BooleanQuery();
        BooleanQuery typesQuery = new BooleanQuery();
        typesQuery.add(new TermQuery(new Term("entityType", ObjectType.DI.getName())), BooleanClause.Occur.SHOULD);
        typesQuery.add(new TermQuery(new Term("entityType", ObjectType.NDI.getName())), BooleanClause.Occur.SHOULD);
        q.add(typesQuery, BooleanClause.Occur.MUST);
        q.add(new TermQuery(new Term("categoryUid", dataCategory.getUid())), BooleanClause.Occur.MUST);
        if (query != null) {
            q.add(query, BooleanClause.Occur.MUST);
        }
        // Do search and fetch Lucene documents.
        // TODO: sort the results
        return getDataItemResultsWrapper(
                getEntityResultsWrapper(
                        luceneService.doSearch(
                                q,
                                filter.getResultStart(),
                                filter.getResultLimit()),
                        filter.isLoadEntityTags(),
                        filter.isLoadMetadatas(),
                        filter.isLoadDataItemValues()));
    }

    // TODO: Find a way to genericise this and the similar method above for Data Categories.
    private ResultsWrapper<DataItem> getDataItemResultsWrapper(ResultsWrapper<IAMEEEntity> entityResultsWrapper) {
        ResultsWrapper<DataItem> dataItemResultsWrapper = new ResultsWrapper<DataItem>();
        dataItemResultsWrapper.setTruncated(entityResultsWrapper.isTruncated());
        dataItemResultsWrapper.setResultStart(entityResultsWrapper.getResultStart());
        dataItemResultsWrapper.setResultLimit(entityResultsWrapper.getResultLimit());
        dataItemResultsWrapper.setHits(entityResultsWrapper.getHits());
        List<DataItem> dataItems = new ArrayList<DataItem>();
        for (IAMEEEntity entity : entityResultsWrapper.getResults()) {
            if (DataItem.class.isAssignableFrom(entity.getClass())) {
                dataItems.add((DataItem) entity);
            } else {
                throw new IllegalStateException("A DataItem was expected.");
            }
        }
        dataItemResultsWrapper.setResults(dataItems);
        return dataItemResultsWrapper;
    }

    // General entity search.

    private ResultsWrapper<IAMEEEntity> getEntityResultsWrapper(
            ResultsWrapper<Document> resultsWrapper,
            boolean loadEntityTags,
            boolean loadMetadata,
            boolean loadItemValues) {
        // Collate entityIds against entityTypes.
        Map<ObjectType, Set<Long>> entityIds = new HashMap<ObjectType, Set<Long>>();
        for (Document document : resultsWrapper.getResults()) {
            Long entityId = new Long(document.getField("entityId").stringValue());
            ObjectType entityType = ObjectType.valueOf(document.getField("entityType").stringValue());
            Set<Long> idSet = entityIds.get(entityType);
            if (idSet == null) {
                idSet = new HashSet<Long>();
                entityIds.put(entityType, idSet);
            }
            idSet.add(entityId);
        }
        // Collate AMEEEntities.
        Map<ObjectType, Map<String, IAMEEEntity>> entities = new HashMap<ObjectType, Map<String, IAMEEEntity>>();
        // Load DataCategories.
        if (entityIds.containsKey(ObjectType.DC)) {
            Map<String, DataCategory> dataCategoriesMap = dataService.getDataCategoryMap(entityIds.get(ObjectType.DC));
            addDataCategories(entities, dataCategoriesMap);
            // Pre-loading of EntityTags, LocaleNames & DataCategories.
            if (loadEntityTags) {
                tagService.loadEntityTagsForDataCategories(dataCategoriesMap.values());
            }
            if (loadMetadata) {
                metadataService.loadMetadatasForDataCategories(dataCategoriesMap.values());
            }
            localeService.loadLocaleNamesForDataCategories(dataCategoriesMap.values());
        }
        // Load DataItems (NuDataItem).
        Set<String> dataItemUids = new HashSet<String>();
        if (entityIds.containsKey(ObjectType.NDI)) {
            // Collate NuDataItem IDs.
            Set<Long> dataItemIds = new HashSet<Long>();
            if (entityIds.containsKey(ObjectType.NDI)) {
                dataItemIds.addAll(entityIds.get(ObjectType.NDI));
            }
            // Load the items.
            Map<String, NuDataItem> dataItemsMap = dataItemService.getDataItemMap(dataItemIds, loadItemValues);
            // Collect UIDs.
            for (NuDataItem dataItem : dataItemsMap.values()) {
                dataItemUids.add(dataItem.getUid());
            }
            // Add to map.
            addNuDataItems(entities, dataItemsMap);
            // Pre-load Metadatas?
            if (loadMetadata) {
                metadataService.loadMetadatasForNuDataItems(dataItemsMap.values());
            }
        }
        // Load DataItems (LegacyDataItem).
        if (entityIds.containsKey(ObjectType.DI)) {
            // Collate LegacyDataItem IDs.
            Set<Long> dataItemIds = new HashSet<Long>();
            if (entityIds.containsKey(ObjectType.DI)) {
                dataItemIds.addAll(entityIds.get(ObjectType.DI));
            }
            // Load the items.
            Map<String, DataItem> dataItemsMap = dataService.getDataItemMap(dataItemIds, loadItemValues);
            // Exclude DI duplicates of NDI.
            for (DataItem dataItem : dataItemsMap.values()) {
                if (dataItemUids.contains(dataItem.getUid())) {
                    dataItemsMap.remove(dataItem.getUid());
                }
            }
            // Add to map.
            addDataItems(entities, dataItemsMap);
            // Pre-load Metadatas?
            if (loadMetadata) {
                metadataService.loadMetadatasForDataItems(dataItemsMap.values());
            }
        }
        // Create result list in relevance order.
        List<IAMEEEntity> results = new ArrayList<IAMEEEntity>();
        for (Document document : resultsWrapper.getResults()) {
            String entityUid = document.getField("entityUid").stringValue();
            ObjectType entityType = ObjectType.valueOf(document.getField("entityType").stringValue());
            // Do we need to make NDI override DI?
            if (entityType.equals(ObjectType.DI)) {
                entityType = ObjectType.NDI;
            }
            // First attempt to find the result.
            IAMEEEntity result = entities.containsKey(entityType) ? entities.get(entityType).get(entityUid) : null;
            // If we failed to find an NDI, or we already have an NDI with the same identity   look for a DI instead.
            if (((result == null) || results.contains(result)) && (entityType.equals(ObjectType.NDI))) {
                // Second attempt to find the result.
                result = entities.containsKey(ObjectType.DI) ? entities.get(ObjectType.DI).get(entityUid) : null;
            }
            // If we have a result and it is not a duplicate, add to the results list.
            if (result != null) {
                if (!results.contains(result)) {
                    results.add(result);
                }
            } else {
                log.warn("getEntities() Entity was missing: " + entityType + " / " + entityUid);
            }
        }
        return new ResultsWrapper<IAMEEEntity>(results, resultsWrapper.isTruncated());
    }
}
