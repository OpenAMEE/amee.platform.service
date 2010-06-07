package com.amee.platform.search;

import org.apache.lucene.search.Query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class QueryFilter implements Serializable {

    private Map<String, Query> queries = new HashMap<String, Query>();
    private boolean loadDataItemValues = false;
    private boolean loadMetadatas = false;
    private boolean loadEntityTags = false;
    private int resultStart = 0;
    private int resultLimit = 0;

    public QueryFilter() {
        super();
        setResultLimit(getResultLimitDefault());
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

    public int getResultStart() {
        return resultStart;
    }

    public void setResultStart(int resultStart) {
        if (resultStart < 0) {
            throw new IllegalArgumentException("resultStart is less than zero");
        }
        this.resultStart = resultStart;
    }

    public int getResultLimit() {
        return resultLimit;
    }

    public void setResultLimit(int resultLimit) {
        if (resultLimit > getResultLimitMax()) {
            throw new IllegalArgumentException("resultLimit is greater than resultLimitMax");
        }
        if (resultLimit < 0) {
            throw new IllegalArgumentException("resultLimit is less than zero");
        }
        this.resultLimit = resultLimit;
    }

    public abstract int getResultLimitDefault();

    public abstract int getResultLimitMax();
}