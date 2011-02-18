package com.amee.service.data;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.AMEEEntityReference;
import com.amee.domain.AMEEStatus;
import com.amee.domain.APIVersion;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DataServiceDAO {

    public DataCategory getRootDataCategory();

    public DataCategory getDataCategoryByPath(IDataCategoryReference parent, String path);

    public DataCategory getDataCategoryByUidWithAnyStatus(String uid);

    public DataCategory getDataCategoryByUidWithActiveStatus(String uid);

    public DataCategory getDataCategoryByUidWithStatus(String uid, AMEEStatus status);

    public DataCategory getDataCategoryByWikiName(String wikiName, AMEEStatus status);

    public DataCategory getDataCategoryWithStatus(DataCategory dataCategory, AMEEStatus status);

    public DataCategory getDataCategory(IDataCategoryReference dataCategory);

    public ResultsWrapper<DataCategory> getDataCategories();

    public ResultsWrapper<DataCategory> getDataCategories(int resultStart, int resultLimit);

    public List<DataCategory> getDataCategories(Set<Long> dataCategoryIds);

    public List<DataCategory> getDataCategoriesModifiedWithin(
            Date modifiedSince,
            Date modifiedUntil);

    public List<DataCategory> getDataCategoriesForDataItemsModifiedWithin(
            Date modifiedSince,
            Date modifiedUntil);

    public Map<String, IDataCategoryReference> getDataCategories(IDataCategoryReference dataCategoryReference);

    public Set<AMEEEntityReference> getDataCategoryReferences(ItemDefinition itemDefinition);

    public Set<Long> getParentDataCategoryIds(Set<Long> dataCategoryIds);

    public boolean isDataCategoryUniqueByPath(DataCategory dataCategory);

    public boolean isDataCategoryUniqueByWikiName(DataCategory dataCategory);

    public void persist(DataCategory dc);

    public void remove(DataCategory dataCategory);

    public void invalidate(DataCategory dataCategory);

    public List<APIVersion> getAPIVersions();

    public APIVersion getAPIVersion(String version);
}
