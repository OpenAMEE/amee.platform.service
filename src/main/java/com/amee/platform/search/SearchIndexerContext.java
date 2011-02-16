package com.amee.platform.search;

import com.amee.domain.item.data.DataItem;
import org.apache.lucene.document.Document;

import java.util.List;

/**
 * A runtime context for a {@link SearchIndexer] session.
 * <p/>
 * TODO: Turn this into normal bean (constructor, setters, getters).
 */
public class SearchIndexerContext {

    // Current Data Category.
    public String dataCategoryUid;

    // Should All Data Categories be updated regardless of modification date?
    public boolean handleDataCategories = false;

    // Should DataItem documents be handled when handling a DataCategory.
    public boolean handleDataItems = false;

    // Should DataItem documents be checked in detail.
    public boolean checkDataItems = false;

    // Work-in-progress List of Data Item Documents.
    public List<Document> dataItemDocs;

    // Current Data Item.
    public DataItem dataItem;

    // Current Data Item Document
    public Document dataItemDoc;

    /**
     * Two {@link SearchIndexerContext}s are equal if their dataCategoryUid is equal.
     *
     * @param o object to compare
     * @return true if the supplied object matches this object
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || !SearchIndexerContext.class.isAssignableFrom(o.getClass())) return false;
        SearchIndexerContext entity = (SearchIndexerContext) o;
        return dataCategoryUid.equals(entity.dataCategoryUid);
    }

    /**
     * Returns a hash code based on the dataCategoryUid.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return dataCategoryUid.hashCode();
    }
}
