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

    public void setQ(Query uid) {
        getQueries().put("q", uid);
    }

    public Set<ObjectType> getTypes() {
        return types;
    }

    public void setTypes(Set<ObjectType> types) {
        if (types != null) {
            this.types = types;
        }
    }
}