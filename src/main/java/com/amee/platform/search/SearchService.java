package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.*;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.data.DataItem;
import com.amee.service.data.DataService;
import com.amee.service.tag.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Reader;
import java.util.*;

public class SearchService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public final static Analyzer STANDARD_ANALYZER = new StandardAnalyzer(Version.LUCENE_30);
    public final static Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();

    /**
     * An Analyzer instance which ensures tokens are in lower case. Uses KeywordTokenizer and LowerCaseFilter.
     */
    public final static Analyzer LOWER_CASE_KEYWORD_ANALYZER = new Analyzer() {
        public TokenStream tokenStream(String fieldName, Reader reader) {
            TokenStream result = new KeywordTokenizer(reader);
            result = new LowerCaseFilter(result);
            return result;
        }
    };

    /**
     * An Analyzer instance to parse tags. Uses TagTokenizer and LowerCaseFilter. Will replace all commas with a
     * space to avoid confusing Lucene.
     */
    public final static Analyzer TAG_ANALYZER = new Analyzer() {
        public TokenStream tokenStream(String fieldName, Reader reader) {
            TokenStream result = new TagTokenizer(reader);
            result = new LowerCaseFilter(result);
            return result;
        }
    };

    /**
     * A Tokenizer to turn tags into tokens. Follows the rules of AMEE Tags. Tags can contain numbers, letters and
     * underscores.
     */
    public final static class TagTokenizer extends CharTokenizer {

        public TagTokenizer(Reader in) {
            super(in);
        }

        public TagTokenizer(AttributeSource source, Reader in) {
            super(source, in);
        }

        public TagTokenizer(AttributeFactory factory, Reader in) {
            super(factory, in);
        }

        @Override
        protected boolean isTokenChar(char c) {
            return Character.isLetterOrDigit(c) || (c == '_');
        }
    }

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

    /**
     * @param filter
     * @return
     */
    public ResultsWrapper<IAMEEEntity> getEntities(SearchFilter filter) {

        // Do search and fetch Lucene documents.
        return getEntityResultsWrapper(
                searchQueryService.doSearch(filter),
                filter.isLoadEntityTags(),
                filter.isLoadMetadatas(),
                filter.isLoadDataItemValues());
    }

    protected void addDataCategories(Map<ObjectType, Map<String, IAMEEEntity>> entities, Map<String, DataCategory> dataCategoriesMap) {
        Map<String, IAMEEEntity> e = new HashMap<String, IAMEEEntity>();
        for (Map.Entry<String, DataCategory> entry : dataCategoriesMap.entrySet()) {
            e.put(entry.getKey(), entry.getValue());
        }
        entities.put(ObjectType.DC, e);
    }

    /**
     * @param entities
     * @param dataItemsMap
     */
    protected void addDataItems(Map<ObjectType, Map<String, IAMEEEntity>> entities, Map<String, DataItem> dataItemsMap) {
        Map<String, IAMEEEntity> dataItems = new HashMap<String, IAMEEEntity>();
        for (Map.Entry<String, DataItem> entry : dataItemsMap.entrySet()) {
            IAMEEEntity entity = entry.getValue();
            if (entity.getObjectType().equals(ObjectType.DI)) {
                dataItems.put(entry.getKey(), entry.getValue());
            } else {
                throw new IllegalStateException("An ObjectType of type '" + ObjectType.DI + "' was expected.");
            }
        }
        entities.put(ObjectType.DI, dataItems);
    }

    // DataCategory Search.

    public ResultsWrapper<DataCategory> getDataCategories(DataCategoriesFilter filter) {
        ResultsWrapper<DataCategory> resultsWrapper;
        // Filter based on an allowed query parameter.
        if (!filter.getQueries().isEmpty()) {
            BooleanQuery query = new BooleanQuery();
            for (Map.Entry<String, Query> entry : filter.getQueries().entrySet()) {
                if (entry.getKey().equals("excTags")) {
                    query.add(entry.getValue(), BooleanClause.Occur.MUST_NOT);
                } else {
                    query.add(entry.getValue(), BooleanClause.Occur.MUST);
                }
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
                                filter.getResultLimit(), LuceneServiceImpl.MAX_NUM_HITS, filter.getSort()),
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
                DataCategory dataCategory = (DataCategory) entity;
                if (!dataCategory.isTrash()) {
                    dataCategories.add((DataCategory) entity);
                }
            } else {
                throw new IllegalStateException("A DataCategory was expected.");
            }
        }
        dataCategoryResultsWrapper.setResults(dataCategories);
        return dataCategoryResultsWrapper;
    }

    // DataItem search.

    public ResultsWrapper<DataItem> getDataItems(DataCategory dataCategory) {
        return getDataItems(dataCategory, new DataItemsFilter());
    }

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

    /**
     * Find all DIs in the DataCategory matching the supplied Query and range.
     * 
     * @param dataCategory
     * @param filter
     * @param query
     * @return
     */
    private ResultsWrapper<DataItem> getDataItems(DataCategory dataCategory, DataItemsFilter filter, Query query) {
        BooleanQuery q = new BooleanQuery();
        BooleanQuery typesQuery = new BooleanQuery();
        typesQuery.add(new TermQuery(new Term("entityType", ObjectType.DI.getName())), BooleanClause.Occur.SHOULD);
        q.add(typesQuery, BooleanClause.Occur.MUST);
        q.add(new TermQuery(new Term("categoryUid", dataCategory.getUid())), BooleanClause.Occur.MUST);
        if (query != null) {
            q.add(query, BooleanClause.Occur.MUST);
        }
        
        // Do search and fetch Lucene documents.
        return getDataItemResultsWrapper(
                getEntityResultsWrapper(
                        luceneService.doSearch(
                                q,
                                filter.getResultStart(),
                                filter.getResultLimit(),
                                LuceneServiceImpl.MAX_NUM_HITS,
                                filter.getSort()),
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
                DataItem dataItem = (DataItem) entity;
                if (!dataItem.isTrash()) {
                    dataItems.add((DataItem) entity);
                }
            } else {
                throw new IllegalStateException("A DataItem was expected.");
            }
        }
        dataItemResultsWrapper.setResults(dataItems);
        return dataItemResultsWrapper;
    }

    // General entity search.

    /**
     * @param resultsWrapper
     * @param loadEntityTags
     * @param loadMetadata
     * @param loadItemValues
     * @return
     */
    private ResultsWrapper<IAMEEEntity> getEntityResultsWrapper(
            ResultsWrapper<Document> resultsWrapper,
            boolean loadEntityTags,
            boolean loadMetadata,
            boolean loadItemValues) {

        // Collate entityIds against entityTypes.
        Map<ObjectType, Set<Long>> entityIds = new HashMap<ObjectType, Set<Long>>();
        for (Document document : resultsWrapper.getResults()) {
            Long entityId = new Long(document.getField("entityId").stringValue());
            ObjectType entityType = ObjectType.fromString(document.getField("entityType").stringValue());
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

            // Load the categories.
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

        // Load DataItems.
        if (entityIds.containsKey(ObjectType.DI)) {

            // Load the items.
            Map<String, DataItem> dataItemsMap = dataItemService.getDataItemMap(entityIds.get(ObjectType.DI), loadItemValues);

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
            ObjectType entityType = ObjectType.fromString(document.getField("entityType").stringValue());

            // First attempt to find the result.
            IAMEEEntity result = entities.containsKey(entityType) ? entities.get(entityType).get(entityUid) : null;

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
