package com.amee.service.data;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.AMEEEntityReference;
import com.amee.domain.AMEEStatus;
import com.amee.domain.APIVersion;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.service.invalidation.InvalidationMessage;
import org.springframework.context.ApplicationListener;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DataService extends ApplicationListener<InvalidationMessage> {

    public DataCategory getRootDataCategory();

    public IDataCategoryReference getDataCategoryByPath(IDataCategoryReference parent, String path);

    public DataCategory getDataCategoryByIdentifier(String identifier);

    public DataCategory getDataCategoryByIdentifier(String identifier, AMEEStatus status);

    public DataCategory getDataCategoryByWikiName(String wikiName);

    public DataCategory getDataCategoryByWikiName(String wikiName, AMEEStatus status);

    public DataCategory getDataCategoryByUid(String uid);

    public DataCategory getDataCategoryByUid(String uid, AMEEStatus status);

    public DataCategory getDataCategory(IDataCategoryReference dataCategory);

    public IDataCategoryReference getDataCategoryByFullPath(String path);

    public IDataCategoryReference getDataCategoryByFullPath(List<String> segments);

    public List<DataCategory> getDataCategories();

    public ResultsWrapper<DataCategory> getDataCategories(int resultStart, int resultLimit);

    public List<DataCategory> getDataCategories(boolean locales);

    public ResultsWrapper<DataCategory> getDataCategories(boolean locales, int resultStart, int resultLimit);

    public Map<String, DataCategory> getDataCategoryMap(Set<Long> dataCategoryIds);

    public List<DataCategory> getDataCategories(Set<Long> dataCategoryIds);

    public List<DataCategory> getDataCategoriesModifiedWithin(Date modifiedSince, Date modifiedUntil);

    public List<DataCategory> getDataCategoriesForDataItemsModifiedWithin(Date modifiedSince, Date modifiedUntil);

    public Map<String, IDataCategoryReference> getDataCategories(IDataCategoryReference dataCategoryReference);

    public Set<Long> getParentDataCategoryIds(Set<Long> dataCategoryIds);

    public Set<AMEEEntityReference> getDataCategoryReferences(ItemDefinition itemDefinition);

    boolean isDataCategoryUniqueByPath(DataCategory dataCategory);

    boolean isDataCategoryUniqueByWikiName(DataCategory dataCategory);

    public Date getDataCategoryModifiedDeep(DataCategory dataCategory);

    public Date getDataItemsModifiedDeep(DataCategory dataCategory);

    void persist(DataCategory dataCategory);

    void remove(DataCategory dataCategory);

    void clearCaches(DataCategory dataCategory);

    public List<APIVersion> getAPIVersions();

    public APIVersion getAPIVersion(String version);
}
