package com.amee.platform.search;

import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import java.util.Arrays;
import java.util.List;

public class DataCategoriesFilter extends QueryFilter {

    private static final List<String> sortableFields = Arrays.asList("uid", "path", "fullPath", "parentUid", "itemDefinitionUid");

    public DataCategoriesFilter() {
        super();
    }

    public Query getUid() {
        return getQueries().get("uid");
    }

    public void setUid(Query uid) {
        getQueries().put("uid", uid);
    }

    public Query getName() {
        return getQueries().get("name");
    }

    public void setName(Query name) {
        getQueries().put("name", name);
    }

    public Query getPath() {
        return getQueries().get("path");
    }

    public void setPath(Query path) {
        getQueries().put("path", path);
    }

    public Query getFullPath() {
        return getQueries().get("fullPath");
    }

    public void setFullPath(Query fullPath) {
        getQueries().put("fullPath", fullPath);
    }

    public Query getWikiName() {
        return getQueries().get("wikiName");
    }

    public void setWikiName(Query wikiName) {
        getQueries().put("wikiName", wikiName);
    }

    public Query getWikiDoc() {
        return getQueries().get("wikiDoc");
    }

    public void setWikiDoc(Query wikiDoc) {
        getQueries().put("wikiDoc", wikiDoc);
    }

    public Query getProvenance() {
        return getQueries().get("provenance");
    }

    public void setProvenance(Query provenance) {
        getQueries().put("provenance", provenance);
    }

    public Query getAuthority() {
        return getQueries().get("authority");
    }

    public void setAuthority(Query authority) {
        getQueries().put("authority", authority);
    }

    public Query getParentUid() {
        return getQueries().get("parentUid");
    }

    public void setParentUid(Query parentUid) {
        getQueries().put("parentUid", parentUid);
    }

    public Query getParentWikiName() {
        return getQueries().get("parentWikiName");
    }

    public void setParentWikiName(Query parentWikiName) {
        getQueries().put("parentWikiName", parentWikiName);
    }

    public Query getItemDefinitionUid() {
        return getQueries().get("itemDefinitionUid");
    }

    public void setItemDefinitionUid(Query itemDefinitionUid) {
        getQueries().put("itemDefinitionUid", itemDefinitionUid);
    }

    public Query getItemDefinitionName() {
        return getQueries().get("itemDefinitionName");
    }

    public void setItemDefinitionName(Query itemDefinitionName) {
        getQueries().put("itemDefinitionName", itemDefinitionName);
    }

    public void setTags(Query tags) {
        getQueries().put("tags", tags);
    }

    public Query removeExcTags() {
        return getQueries().remove("excTags");
    }

    public void setExcTags(Query tags) {
        getQueries().put("excTags", tags);
    }

    @Override
    public int getResultLimitDefault() {
        return 50;
    }

    @Override
    public int getResultLimitMax() {
        return 100;
    }

    @Override
    public Sort getSort() {

        // If there is no filtering, sort by label
        if (getQueries().isEmpty()) {
            return new Sort(new SortField("byWikiName", SortField.STRING));
        }

        // If there are only sortable fields, sort by label, otherwise, sort by relevance
        if (CollectionUtils.subtract(getQueries().keySet(), sortableFields).isEmpty()) {
            return new Sort(new SortField("byWikiName", SortField.STRING));
        } else {
            return Sort.RELEVANCE;
        }
    }
}
