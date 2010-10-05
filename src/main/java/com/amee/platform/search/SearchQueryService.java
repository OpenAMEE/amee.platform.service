package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.AMEEEntity;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class SearchQueryService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private LuceneService luceneService;

    protected ResultsWrapper<Document> doSearch(SearchFilter filter) {
        Query primaryQuery;
        // Obtain Query - Do we need to query with types?
        if (!filter.getTypes().isEmpty()) {
            // Only search specified types.
            primaryQuery = getQueryFilteredByEntityType(filter.getQ(), filter.getTypes());
        } else {
            // Search all entities with filter query.
            primaryQuery = filter.getQ();
        }
        // Get Lucene Documents matching query within page range.
        ResultsWrapper<Document> pagedPrimaryResults = luceneService.doSearch(primaryQuery, filter.getResultStart(), filter.getResultLimit());
        // Would we like more results (not truncated)? Are we only searching for Data Categories?
        if (!pagedPrimaryResults.isTruncated() &&
                filter.getTypes().contains(ObjectType.DC) && filter.getTypes().size() == 1) {
            // Attempt to supplement Data Category results with matches on Data Items.
            // Search for Data Items.
            ResultsWrapper<Document> allSecondaryResults =
                    luceneService.doSearch(
                            getQueryFilteredByEntityType(filter.getQ(), ObjectType.DI));
            // Only handle secondary results if some where found.
            if (!allSecondaryResults.getResults().isEmpty()) {
                // Get all Documents matching primary query (this is a duplicate of the search above).
                ResultsWrapper<Document> allPrimaryResults = luceneService.doSearch(primaryQuery);
                // Collect primary results Data Category UIDs.
                List<String> primaryDataCategoryUids = new ArrayList<String>();
                for (Document d : allPrimaryResults.getResults()) {
                    primaryDataCategoryUids.add(d.getField("entityUid").stringValue());
                }
                // Collect secondary results Data Category UIDs.
                List<String> secondaryDataCategoryUids = new ArrayList<String>();
                for (Document d : allSecondaryResults.getResults()) {
                    Field f = d.getField("categoryUid");
                    if (f != null) {
                        // Add the Data Category UID if it is not already present.
                        String uid = d.getField("categoryUid").stringValue();
                        if (!primaryDataCategoryUids.contains(uid) && !secondaryDataCategoryUids.contains(uid)) {
                            secondaryDataCategoryUids.add(uid);
                        }
                    } else {
                        log.warn("doSearch() Data Item Document 'categoryUid' Field does not exist for DI: " + d.getField("entityUid"));
                    }
                }
                // Work out the new resultStart (offset) for the Data Item list.
                int newResultStart = filter.getResultStart() - pagedPrimaryResults.getHits();
                if (newResultStart < 0) {
                    newResultStart = 0;
                }
                // Work out the new resultEnd for the Data Item List.
                int newResultEnd = newResultStart + filter.getResultLimit() + 1;
                if (newResultEnd > secondaryDataCategoryUids.size()) {
                    newResultEnd = secondaryDataCategoryUids.size();
                }
                // Are there Data Categories to add?
                if (newResultStart > newResultEnd) {
                    // No Data Categories, do nothing.
                } else {
                    // There are some Data Categories to add.
                    // Gather Data Category Documents.
                    List<Document> dataCategoryDocuments = new ArrayList<Document>();
                    for (String uid : secondaryDataCategoryUids.subList(newResultStart, newResultEnd)) {
                        Document document = getDocument(ObjectType.DC, uid);
                        if (document != null) {
                            dataCategoryDocuments.add(document);
                        }
                    }
                    // Now add Data Category Documents to main ResultsWrapper.
                    // Fill up ResultsWrapper.results up to a max of the resultLimit.
                    for (Document document : dataCategoryDocuments) {
                        if (pagedPrimaryResults.getResults().size() < filter.getResultLimit()) {
                            pagedPrimaryResults.getResults().add(document);
                        } else {
                            // We had to trim the results.
                            pagedPrimaryResults.setTruncated(true);
                        }
                    }
                }
                // Note: Our ResultsWrapper document list may now contain duplicates which need to be removed higher up the stack.
            }
        }
        return pagedPrimaryResults;
    }

    private Query getQueryFilteredByEntityType(Query q, ObjectType type) {
        return getQueryFilteredByEntityType(q, new HashSet<ObjectType>(Arrays.asList(type)));
    }

    private Query getQueryFilteredByEntityType(Query q, Set<ObjectType> types) {
        // First - add entityType queries.
        BooleanQuery typesQuery = new BooleanQuery();
        for (ObjectType objectType : types) {
            typesQuery.add(new TermQuery(new Term("entityType", objectType.getName())), BooleanClause.Occur.SHOULD);
        }
        // Second - combine filter query and types query.
        BooleanQuery combinedQuery = new BooleanQuery();
        combinedQuery.add(typesQuery, BooleanClause.Occur.MUST);
        combinedQuery.add(q, BooleanClause.Occur.MUST);
        return combinedQuery;
    }

    /**
     * Find a single Lucene Document that matches the supplied entity.
     *
     * @param entity to search for
     * @return Document matching entity or null
     */
    protected Document getDocument(AMEEEntity entity) {
        return getDocument(entity.getObjectType(), entity.getUid());
    }

    /**
     * Find a single Lucene Document that matches the supplied entity.
     *
     * @param objectType of entity to search for
     * @param uid        if entity to search for
     * @return Document matching entity or null
     */
    protected Document getDocument(ObjectType objectType, String uid) {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("entityType", objectType.getName())), BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("entityUid", uid)), BooleanClause.Occur.MUST);
        ResultsWrapper<Document> resultsWrapper = luceneService.doSearch(query, 0, 1);
        if (resultsWrapper.isTruncated()) {
            log.warn("getDocument() Entity in index more than once: " + objectType + "/" + uid);
        }
        if (!resultsWrapper.getResults().isEmpty()) {
            return resultsWrapper.getResults().get(0);
        } else {
            return null;
        }
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
}
