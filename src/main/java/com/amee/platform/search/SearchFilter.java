package com.amee.platform.search;

import com.amee.domain.ObjectType;
import org.apache.lucene.search.Query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SearchFilter implements Serializable {

    private Map<String, Query> queries = new HashMap<String, Query>();
    private Set<ObjectType> types = new HashSet<ObjectType>();
    private boolean loadDataItemValues = false;

    public SearchFilter() {
        super();
    }

    public Query getQ() {
        return getQueries().get("q");
    }

    public void setQ(Query uid) {
        getQueries().put("q", uid);
    }

    public Map<String, Query> getQueries() {
        return queries;
    }

    public void setQueries(Map<String, Query> queries) {
        if (queries != null) {
            this.queries = queries;
        }
    }

    public Set<ObjectType> getTypes() {
        return types;
    }

    public void setTypes(Set<ObjectType> types) {
        if (types != null) {
            this.types = types;
        }
    }

    public boolean isLoadDataItemValues() {
        return loadDataItemValues;
    }

    public void setLoadDataItemValues(boolean loadDataItemValues) {
        this.loadDataItemValues = loadDataItemValues;
    }
}