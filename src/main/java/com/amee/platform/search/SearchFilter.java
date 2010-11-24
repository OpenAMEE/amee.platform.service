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

    public boolean hasTags() {
        return getTags() != null;
    }

    public void setTags(Query tags) {
        getQueries().put("tags", tags);
    }

    public Query getExcTags() {
        return getQueries().get("excTags");
    }

    public boolean hasExcTags() {
        return getExcTags() != null;
    }

    public void setExcTags(Query tags) {
        getQueries().put("excTags", tags);
    }

    public Set<ObjectType> getTypes() {
        return types;
    }

    public boolean hasTypes() {
        return hasTypes(types);
    }

    public static boolean hasTypes(Set<ObjectType> types) {
        return (types != null) && !types.isEmpty();
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
        // Do we need to create a combined query?
        if (hasTags() || hasExcTags() || hasTypes(types)) {
            // Create a combined query.
            BooleanQuery combinedQuery = new BooleanQuery();
            // Add refinement queries.
            addRefinementQueries(combinedQuery, types);
            // Add plain search query.
            return addSearchQuery(combinedQuery);
        } else {
            // Just return the plain query.
            return getQ();
        }
    }

    public Query addRefinementQueries(BooleanQuery combinedQuery) {
        addRefinementQueries(combinedQuery, getTypes());
        return combinedQuery;
    }

    public Query addRefinementQueries(BooleanQuery combinedQuery, Set<ObjectType> types) {
        // First - add entityType queries.
        if (hasTypes(types)) {
            BooleanQuery typesQuery = new BooleanQuery();
            for (ObjectType objectType : types) {
                typesQuery.add(new TermQuery(new Term("entityType", objectType.getName())), BooleanClause.Occur.SHOULD);
            }
            combinedQuery.add(typesQuery, BooleanClause.Occur.MUST);
        }
        // Second - add tags query.
        if (hasTags()) {
            combinedQuery.add(getTags(), BooleanClause.Occur.MUST);
        }
        // Third - add excluded tags.
        if (hasExcTags()) {
            combinedQuery.add(getExcTags(), BooleanClause.Occur.MUST_NOT);
        }
        return combinedQuery;
    }

    public Query addSearchQuery(BooleanQuery combinedQuery) {
        combinedQuery.add(getQ(), BooleanClause.Occur.MUST);
        return combinedQuery;
    }

    public int getResultLimitDefault() {
        return 25;
    }

    public int getResultLimitMax() {
        return 50;
    }
}