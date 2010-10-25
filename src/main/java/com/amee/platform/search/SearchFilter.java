package com.amee.platform.search;

import com.amee.domain.ObjectType;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.Arrays;
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

    public Query getExcTags() {
        return getQueries().get("excTags");
    }

    public void setExcTags(Query tags) {
        getQueries().put("excTags", tags);
    }

    public Set<ObjectType> getTypes() {
        return types;
    }

    public void setTypes(Set<ObjectType> types) {
        if (types != null) {
            this.types = types;
        }
    }

    public Query getQuery(ObjectType type1, ObjectType type2) {
        return getQuery(new HashSet<ObjectType>(Arrays.asList(type1, type2)));
    }

    public Query getQuery() {
        return getQuery(getTypes());
    }

    public Query getQuery(Set<ObjectType> types) {
        Query q = getQuery(),
              tags = getTags(),
              excTags = getExcTags();

        // Do we need to create a combined query?
        if ((tags != null) || (types != null) && !types.isEmpty()) {
            // Create a combined query.
            BooleanQuery combinedQuery = new BooleanQuery();
            // First - add entityType queries.
            if ((types != null) && !types.isEmpty()) {
                BooleanQuery typesQuery = new BooleanQuery();
                for (ObjectType objectType : types) {
                    typesQuery.add(new TermQuery(new Term("entityType", objectType.getName())), BooleanClause.Occur.SHOULD);
                }
                combinedQuery.add(typesQuery, BooleanClause.Occur.MUST);
            }
            // Second - add tags query.
            if (tags != null) {
                combinedQuery.add(tags, BooleanClause.Occur.MUST);
            }
            // Third - add excluded tags
            if (excTags != null) {
                combinedQuery.add(excTags, BooleanClause.Occur.MUST_NOT);
            }
            // Finally - add plain query.
            combinedQuery.add(q, BooleanClause.Occur.MUST);
            return combinedQuery;
        } else {
            // Just return the simple query.
            return q;
        }
    }

    public int getResultLimitDefault() {
        return 25;
    }

    public int getResultLimitMax() {
        return 50;
    }
}