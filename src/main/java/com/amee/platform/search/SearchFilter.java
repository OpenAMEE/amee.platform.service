package com.amee.platform.search;

import com.amee.domain.ObjectType;
import org.apache.lucene.search.Query;

import java.util.HashSet;
import java.util.Set;

public class SearchFilter extends QueryFilter {

    private Set<ObjectType> types = new HashSet<ObjectType>();

    public SearchFilter() {
        super();
    }

    public Query getQ() {
        return getQueries().get("q");
    }

    public void setQ(Query query) {
        getQueries().put("q", query);
    }

    public Query getTags() {
        return getQueries().get("tags");
    }

    public void setTags(Query tags) {
        getQueries().put("tags", tags);
    }

    public Set<ObjectType> getTypes() {
        return types;
    }

    public void setTypes(Set<ObjectType> types) {
        if (types != null) {
            this.types = types;
        }
    }

    public int getResultLimitDefault() {
        return 25;
    }

    public int getResultLimitMax() {
        return 50;
    }
}