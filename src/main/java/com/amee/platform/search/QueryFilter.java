package com.amee.platform.search;

import org.apache.lucene.search.Query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class QueryFilter implements Serializable {

    private Map<String, Query> queries = new HashMap<String, Query>();
    private boolean loadDataItemValues = false;
    private boolean loadMetadatas = false;

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
}