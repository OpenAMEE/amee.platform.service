package com.amee.platform.search;

import org.apache.lucene.search.Query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class QueryFilter implements Serializable {

    private Map<String, Query> queries = new HashMap<String, Query>();

    public QueryFilter() {
        super();
    }


    public Map<String, Query> getQueries() {
        return queries;
    }

    public void setQueries(Map<String, Query> queries) {
        this.queries = queries;
    }
}