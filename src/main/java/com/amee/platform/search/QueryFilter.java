package com.amee.platform.search;

import com.amee.domain.LimitFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import java.util.HashMap;
import java.util.Map;

public abstract class QueryFilter extends LimitFilter {

    private Map<String, Query> queries = new HashMap<String, Query>();
    private boolean loadDataItemValues = false;
    private boolean loadMetadatas = false;
    private boolean loadEntityTags = false;

    public QueryFilter() {
        super();
    }

    public Map<String, Query> getQueries() {
        return queries;
    }

    public void setQueries(Map<String, Query> queries) {
        if (queries != null) {
            this.queries = queries;
        }
    }

    public boolean isLoadDataItemValues() {
        return loadDataItemValues;
    }

    public void setLoadDataItemValues(boolean loadDataItemValues) {
        this.loadDataItemValues = loadDataItemValues;
    }

    public boolean isLoadMetadatas() {
        return loadMetadatas;
    }

    public void setLoadMetadatas(boolean loadMetadatas) {
        this.loadMetadatas = loadMetadatas;
    }

    public boolean isLoadEntityTags() {
        return loadEntityTags;
    }

    public void setLoadEntityTags(boolean loadEntityTags) {
        this.loadEntityTags = loadEntityTags;
    }

    /**
     * Get a Sort object that encapsulates an ordered collection of field sorting information.
     * To sort by relevance (the default) you may return Sort.RELEVANCE however there is some overhead in using a Sort object.
     *
     * @return A Sort object for ordering results.
     */
    public Sort getSort() {
        return Sort.RELEVANCE;
    }
}