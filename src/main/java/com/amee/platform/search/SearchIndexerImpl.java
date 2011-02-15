package com.amee.platform.search;

import com.amee.base.transaction.AMEETransaction;
import com.amee.domain.IAMEEEntity;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItem;
import com.amee.platform.science.Amount;
import com.amee.service.data.DataService;
import com.amee.service.invalidation.InvalidationService;
import com.amee.service.item.DataItemService;
import com.amee.service.locale.LocaleService;
import com.amee.service.metadata.MetadataService;
import com.amee.service.tag.TagService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.util.*;

public class SearchIndexerImpl implements SearchIndexer {

    private final Log log = LogFactory.getLog(getClass());
    private final Log searchLog = LogFactory.getLog("search");

    public final static DateTimeFormatter DATE_TO_SECOND = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    // Count of successfully indexed DataCategories.
    private static long COUNT = 0L;

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

    @Autowired
    private InvalidationService invalidationService;

    // A wrapper object encapsulating the context of the current indexing operation.
    private SearchIndexerContext documentContext;

    // The DataCategory currently being indexed.
    private DataCategory dataCategory;

    /**
     * Was the index cleared on start?
     */
    private boolean indexCleared = false;

    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void handleDocumentContext() {
        try {
            searchLog.info(documentContext.dataCategoryUid + "|Started processing DataCategory.");
            // Get the DataCategory and handle.
            dataCategory = dataService.getDataCategoryByUid(documentContext.dataCategoryUid, null);
            if (dataCategory != null) {
                updateDataCategory();
            } else {
                searchLog.warn(documentContext.dataCategoryUid + "|DataCategory not found.");
            }
        } catch (Throwable t) {
            searchLog.error(documentContext.dataCategoryUid + "|Error processing DataCategory.");
            log.error("run() Caught Throwable: " + t.getMessage(), t);
        } finally {
            // We're done!
            searchLog.info(documentContext.dataCategoryUid + "|Completed processing DataCategory.");
        }
    }

    /**
     * Update or remove Data Category & Data Items from the search index.
     */
    protected void updateDataCategory() {
        if (!dataCategory.isTrash()) {
            Document document = searchQueryService.getDocument(dataCategory, true);
            if (document != null) {
                Field modifiedField = document.getField("entityModified");
                if (modifiedField != null) {
                    DateTime modifiedInIndex =
                            DATE_TO_SECOND.parseDateTime(modifiedField.stringValue());
                    DateTime modifiedInDatabase =
                            new DateTime(dataCategory.getModified()).withMillisOfSecond(0);
                    if (documentContext.handleDataCategories || documentContext.handleDataItems || modifiedInDatabase.isAfter(modifiedInIndex)) {
                        searchLog.info(documentContext.dataCategoryUid + "|DataCategory has been modified or re-index requested, updating.");
                        handleDataCategory();
                    } else {
                        searchLog.info(documentContext.dataCategoryUid + "|DataCategory is up-to-date, skipping.");
                    }
                } else {
                    searchLog.info(documentContext.dataCategoryUid + "|The DataCategory modified field was missing, updating");
                    handleDataCategory();
                }
            } else {
                searchLog.info(documentContext.dataCategoryUid + "|DataCategory not in index, adding for the first time.");
                documentContext.handleDataItems = true;
                handleDataCategory();
            }
        } else {
            searchLog.info(documentContext.dataCategoryUid + "|DataCategory needs to be removed.");
            searchQueryService.removeDataCategory(dataCategory);
            searchQueryService.removeDataItems(dataCategory);
            // Send message stating that the DataCategory has been re-indexed.
            invalidationService.add(dataCategory, "dataCategoryIndexed");
        }
    }

    /**
     * Add Documents for the supplied Data Category and any associated Data Items to the Lucene index.
     */
    protected void handleDataCategory() {
        log.debug("handleDataCategory() " + dataCategory.toString());
        // Handle Data Items (Create, store & update documents).
        if (documentContext.handleDataItems) {
            handleDataItems();
        }
        // Get Data Category Document.
        Document dataCategoryDoc = getDocumentForDataCategory(dataCategory);
        // Are we working with an existing index?
        if (!indexCleared) {
            // Update the Data Category Document (remove old Documents).
            luceneService.updateDocument(
                    dataCategoryDoc,
                    new Term("entityType", ObjectType.DC.getName()),
                    new Term("entityUid", dataCategory.getUid()));
        } else {
            // Add the new Document.
            luceneService.addDocument(dataCategoryDoc);
        }
        // Send message stating that the DataCategory has been re-indexed.
        invalidationService.add(dataCategory, "dataCategoryIndexed");
        // Increment count for DataCategories successfully indexed.
        incrementCount();
    }

    /**
     * Create all DataItem documents for the supplied DataCategory.
     */
    protected void handleDataItems() {
        documentContext.dataItemDoc = null;
        documentContext.dataItemDocs = null;
        // There are only Data Items for a Data Category if there is an Item Definition.
        if (dataCategory.getItemDefinition() != null) {
            log.info("handleDataItems() Starting... (" + dataCategory.toString() + ")");
            // Pre-cache metadata and locales for the Data Items.
            metadataService.loadMetadatasForItemValueDefinitions(dataCategory.getItemDefinition().getItemValueDefinitions());
            localeService.loadLocaleNamesForItemValueDefinitions(dataCategory.getItemDefinition().getItemValueDefinitions());
            List<DataItem> dataItems = dataItemService.getDataItems(dataCategory, false);
            metadataService.loadMetadatasForDataItems(dataItems);
            // Iterate over all Data Items and create Documents.
            documentContext.dataItemDocs = new ArrayList<Document>();
            for (DataItem dataItem : dataItems) {
                documentContext.dataItem = dataItem;
                // Create new Data Item Document.
                documentContext.dataItemDoc = getDocumentForDataItem(dataItem);
                documentContext.dataItemDocs.add(documentContext.dataItemDoc);
                // Handle the Data Item Values.
                handleDataItemValues(documentContext);
            }
            // Clear caches.
            metadataService.clearMetadatas();
            localeService.clearLocaleNames();
            // Are we working with an existing index?
            if (!indexCleared) {
                // Clear existing Data ItemItem Documents for this DataCategory.
                searchQueryService.removeDataItems(dataCategory);
            }
            // Add the new Data Item Documents to the index (if any).
            luceneService.addDocuments(documentContext.dataItemDocs);
            log.info("handleDataItems() ...done (" + dataCategory.toString() + ").");
        } else {
            log.debug("handleDataItems() DataCategory does not have items: " + dataCategory.toString());
            // Ensure we clear any Data Item Documents for this Data Category.
            searchQueryService.removeDataItems(dataCategory);
        }
    }

    // Lucene Document creation.

    protected Document getDocumentForDataCategory(DataCategory dataCategory) {
        Document doc = getDocumentForAMEEEntity(dataCategory);
        doc.add(new Field("name", dataCategory.getName().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("path", dataCategory.getPath().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("fullPath", dataCategory.getFullPath().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("wikiName", dataCategory.getWikiName().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("byWikiName", dataCategory.getWikiName().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("wikiDoc", dataCategory.getWikiDoc().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("provenance", dataCategory.getProvenance().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("authority", dataCategory.getAuthority().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        if (dataCategory.getDataCategory() != null) {
            doc.add(new Field("parentUid", dataCategory.getDataCategory().getUid(), Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field("parentWikiName", dataCategory.getDataCategory().getWikiName().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        }
        if (dataCategory.getItemDefinition() != null) {
            doc.add(new Field("itemDefinitionUid", dataCategory.getItemDefinition().getUid(), Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field("itemDefinitionName", dataCategory.getItemDefinition().getName().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        }
        doc.add(new Field("tags", new SearchService.TagTokenizer(new StringReader(tagService.getTagsCSV(dataCategory).toLowerCase()))));
        return doc;
    }

    protected Document getDocumentForDataItem(DataItem dataItem) {
        Document doc = getDocumentForAMEEEntity(dataItem);
        doc.add(new Field("name", dataItem.getName().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("path", dataItem.getPath().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("fullPath", dataItem.getFullPath().toLowerCase() + "/" + dataItem.getDisplayPath().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("wikiDoc", dataItem.getWikiDoc().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("provenance", dataItem.getProvenance().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("categoryUid", dataItem.getDataCategory().getUid(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("categoryWikiName", dataItem.getDataCategory().getWikiName().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("itemDefinitionUid", dataItem.getItemDefinition().getUid(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("itemDefinitionName", dataItem.getItemDefinition().getName().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        for (BaseItemValue itemValue : dataItemService.getItemValues(dataItem)) {
            if (itemValue.isUsableValue()) {
                if (itemValue.getItemValueDefinition().isDrillDown()) {
                    doc.add(new Field(itemValue.getDisplayPath(), itemValue.getValueAsString().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
                    doc.add(new Field(itemValue.getDisplayPath() + "_drill", itemValue.getValueAsString(), Field.Store.YES, Field.Index.NO));
                } else {
                    if (itemValue.isDouble()) {
                        try {
                            doc.add(new NumericField(itemValue.getDisplayPath()).setDoubleValue(new Amount(itemValue.getValueAsString()).getValue()));
                        } catch (NumberFormatException e) {
                            log.warn("getDocumentForDataItem() Could not parse '" + itemValue.getDisplayPath() + "' value '" + itemValue.getValueAsString() + "' for DataItem " + dataItem.toString() + ".");
                            doc.add(new Field(itemValue.getDisplayPath(), itemValue.getValueAsString().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
                        }
                    } else {
                        doc.add(new Field(itemValue.getDisplayPath(), itemValue.getValueAsString().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
                    }
                }
            }
        }
        doc.add(new Field("label", dataItemService.getLabel(dataItem).toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field("byLabel", dataItemService.getLabel(dataItem).toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field("tags", new SearchService.TagTokenizer(new StringReader(tagService.getTagsCSV(dataItem.getDataCategory()).toLowerCase()))));
        return doc;
    }

    protected Document getDocumentForAMEEEntity(IAMEEEntity entity) {
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

    protected void handleDataItemValues(SearchIndexerContext ctx) {
        for (BaseItemValue itemValue : dataItemService.getItemValues(ctx.dataItem)) {
            if (itemValue.isUsableValue()) {
                if (itemValue.getItemValueDefinition().isDrillDown()) {
                    ctx.dataItemDoc.add(new Field(itemValue.getDisplayPath(), itemValue.getValueAsString().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
                } else {
                    if (itemValue.isDouble()) {
                        try {
                            ctx.dataItemDoc.add(new NumericField(itemValue.getDisplayPath()).setDoubleValue(new Amount(itemValue.getValueAsString()).getValue()));
                        } catch (NumberFormatException e) {
                            log.warn("handleDataItemValues() Could not parse '" + itemValue.getDisplayPath() + "' value '" + itemValue.getValueAsString() + "' for DataItem " + ctx.dataItem.toString() + ".");
                            ctx.dataItemDoc.add(new Field(itemValue.getDisplayPath(), itemValue.getValueAsString().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
                        }
                    } else {
                        ctx.dataItemDoc.add(new Field(itemValue.getDisplayPath(), itemValue.getValueAsString().toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
                    }
                }
            }
        }
    }

    /**
     * Checks Data Items for the current Data Category for inconsistency in the index. This is a convenience
     * wrapper for the areDataCategoryDataItemsInconsistent(List<DataItem>) method below.
     *
     * @return true if the index is inconsistent, otherwise false
     */
    protected boolean areDataCategoryDataItemsInconsistent() {
        return areDataCategoryDataItemsInconsistent(dataItemService.getDataItems(dataCategory, false));
    }

    /**
     * Checks Data Items for the current Data Category for inconsistency in the index. The index is
     * inconsistent if the number of database and index Data Items is different or the most recently
     * modified timestamp for the database Data Items and index Data Items are different.
     *
     * @param dataItems to check
     * @return true if the index is inconsistent, otherwise false
     */
    protected boolean areDataCategoryDataItemsInconsistent(List<DataItem> dataItems) {

        // Create Query for all Data Items within the current DataCategory.
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("entityType", ObjectType.NDI.getName())), BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term("categoryUid", dataCategory.getEntityUid())), BooleanClause.Occur.MUST);

        // Do search. Very high maxNumHits to cope with large Data Categories.
        List<Document> results = luceneService.doSearch(query, 100000).getResults();

        // First: Are the correct number of Data Items in the index?
        if (dataItems.size() != results.size()) {
            log.warn("isDataCategoryCorrupt() Inconsistent DataItem count (DB=" + dataItems.size() + ", Index=" + results.size() + ")");
            return true;
        }

        // Second: Check to see if modified timestamps of the most recently updated items
        // match. If they do, we consider the index to correctly reflect the db.

        // Sort the DataItems by modified timestamp.
        Collections.sort(dataItems, new Comparator<DataItem>() {
            public int compare(DataItem di1, DataItem di2) {
                return di1.getModified().compareTo(di2.getModified());
            }
        });

        // Get the most recently modified DataItem.
        DataItem mostRecentDataItem = dataItems.get(dataItems.size() - 1);
        Date dataItemModified = mostRecentDataItem.getModified();

        // Sort the DataItem documents by modified timestamp.
        try {
            Collections.sort(results, new Comparator<Document>() {
                public int compare(Document doc1, Document doc2) {
                    // Find modified fields.
                    Field doc1Mf = doc1.getField("entityModified");
                    Field doc2Mf = doc2.getField("entityModified");
                    // All docs should have a valid entityModified.
                    if (doc1Mf == null || doc2Mf == null) {
                        // Trigger a rebuild if this is not the case.
                        throw new RuntimeException("areDataCategoryDataItemsInconsistent() A null entityModified of DataItem detected.");
                    } else {
                        // Compare the modified values.
                        DateTime doc1Mod = DATE_TO_SECOND.parseDateTime(doc1Mf.stringValue());
                        DateTime doc2Mod = DATE_TO_SECOND.parseDateTime(doc2Mf.stringValue());
                        return doc1Mod.compareTo(doc2Mod);
                    }
                }
            });
        } catch (RuntimeException e) {
            // Trigger a rebuild on error. Was the entityModified missing?
            return true;
        }

        // Get the most recent Data Item document.
        Document mostRecentDocument = results.get(results.size() - 1);
        Field modField = mostRecentDocument.getField("entityModified");
        Date dataItemDocumentModified = DATE_TO_SECOND.parseDateTime(modField.stringValue()).toDate();

        // Inconsistent if the actual Data Item and Data Item Document have different modified timestamps.
        return !dataItemModified.equals(dataItemDocumentModified);
    }

    public static long getCount() {
        return COUNT;
    }

    public synchronized static void resetCount() {
        COUNT = 0;
    }

    private synchronized static void incrementCount() {
        COUNT++;
    }

    public void setDocumentContext(SearchIndexerContext documentContext) {
        this.documentContext = documentContext;
    }

    @Value("#{ systemProperties['amee.clearIndex'] }")
    public void setIndexCleared(Boolean indexCleared) {
        this.indexCleared = indexCleared;
    }

}