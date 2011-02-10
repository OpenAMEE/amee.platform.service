package com.amee.platform.search;

import com.amee.domain.item.data.NuDataItem;
import org.apache.lucene.document.Document;

import java.util.List;

public class SearchIndexerContext {

    // Current Data Category.
    public String dataCategoryUid;

    // Should All Data Categories be updated regardless of modification date?
    public boolean handleDataCategories = false;

    // Should Data Item documents be handled when handling a Data Category.
    public boolean handleDataItems = false;

    // Work-in-progress List of Data Item Documents.
    public List<Document> dataItemDocs;

    // Current Data Item.
    public NuDataItem dataItem;

    // Current Data Item Document
    public Document dataItemDoc;
}
