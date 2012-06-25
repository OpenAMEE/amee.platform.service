package com.amee.service.data;

import com.amee.base.domain.ResultsWrapper;
import com.amee.base.transaction.AMEETransaction;
import com.amee.base.utils.UidGen;
import com.amee.domain.AMEEEntityReference;
import com.amee.domain.AMEEStatus;
import com.amee.domain.APIVersion;
import com.amee.domain.DataItemService;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.IDataService;
import com.amee.domain.LocaleService;
import com.amee.domain.ObjectType;
import com.amee.domain.cache.CacheHelper;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.service.invalidation.InvalidationMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primary service interface to Data Resources.
 */
@Service("dataService")
public class DataServiceImpl implements DataService, IDataService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DrillDownService drillDownService;

    @Autowired
    private LocaleService localeService;

    @Autowired
    private DataItemService dataItemService;

    @Autowired
    private DataServiceDAOImpl dao;

    private CacheHelper cacheHelper = CacheHelper.getInstance();

    @Override
    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void onApplicationEvent(InvalidationMessage invalidationMessage) {
        if ((invalidationMessage.isLocal() || invalidationMessage.isFromOtherInstance()) &&
                invalidationMessage.getObjectType().equals(ObjectType.DC)) {
            log.trace("onApplicationEvent() Handling InvalidationMessage.");
            DataCategory dataCategory = getDataCategoryByUid(invalidationMessage.getEntityUid(), null);
            if (dataCategory != null) {
                clearCaches(dataCategory);
            }
        }
    }

    @Override
    public DataCategory getRootDataCategory() {
        return dao.getRootDataCategory();
    }

    @Override
    public IDataCategoryReference getDataCategoryByPath(IDataCategoryReference parent, String path) {
        return getDataCategories(parent).get(path);
    }

    @Override
    public DataCategory getDataCategoryByIdentifier(String identifier) {
        return getDataCategoryByIdentifier(identifier, AMEEStatus.ACTIVE);
    }

    @Override
    public DataCategory getDataCategoryByIdentifier(String identifier, AMEEStatus status) {
        DataCategory dataCategory = null;
        if (UidGen.INSTANCE_12.isValid(identifier)) {
            dataCategory = getDataCategoryByUid(identifier, status);
        }
        if (dataCategory == null) {
            dataCategory = getDataCategoryByWikiName(identifier, status);
        }
        return dataCategory;
    }

    @Override
    public DataCategory getDataCategoryByWikiName(String wikiName) {
        return getDataCategoryByWikiName(wikiName, AMEEStatus.ACTIVE);
    }

    @Override
    public DataCategory getDataCategoryByWikiName(String wikiName, AMEEStatus status) {
        return dao.getDataCategoryWithStatus(dao.getDataCategoryByWikiName(wikiName, status), status);
    }

    @Override
    public DataCategory getDataCategoryByUid(String uid) {
        return dao.getDataCategoryByUidWithActiveStatus(uid);
    }

    @Override
    public DataCategory getDataCategoryByUid(String uid, AMEEStatus status) {
        return dao.getDataCategoryByUidWithStatus(uid, status);
    }

    /**
     * Get full DataCategory entity based on the supplied IDataCategoryReference.
     *
     * @param dataCategory IDataCategoryReference to fetch a DataCategory for
     * @return the DataCategory matching the IDataCategoryReference
     */
    @Override
    public DataCategory getDataCategory(IDataCategoryReference dataCategory) {
        return dao.getDataCategory(dataCategory);
    }

    @Override
    public IDataCategoryReference getDataCategoryByFullPath(String path) {
        IDataCategoryReference dataCategory = null;
        if (!StringUtils.isBlank(path)) {
            dataCategory = getDataCategoryByFullPath(new ArrayList<String>(Arrays.asList(path.split("/"))));
        }
        return dataCategory;
    }

    @Override
    public IDataCategoryReference getDataCategoryByFullPath(List<String> segments) {
        IDataCategoryReference dataCategory = null;
        if ((segments != null) && !segments.isEmpty()) {
            // Start with the root DataCategory.
            dataCategory = getRootDataCategory();
            // Loop over all path segments and handle each.
            for (String segment : segments) {
                // We're looking for Data Categories.
                dataCategory = getDataCategoryByPath(dataCategory, segment);
                if (dataCategory == null) {
                    break;
                }
            }
            // At this point dataCategory will either reference the last DataCategory found or be null.
            // It's not possible for dataCategory to reference the root DataCategory as there will
            // have been at least one path segment.
        }
        return dataCategory;
    }

    @Override
    public List<DataCategory> getDataCategories() {
        return getDataCategories(false);
    }

    @Override
    public List<DataCategory> getDataCategories(boolean locales) {
        return getDataCategories(locales, 0, 0).getResults();
    }

    /**
     * TODO: There is a potential for confusing pagination truncation when removing inactive entries.
     * TODO: The database query already excludes trashed categories, why do we check again?
     *
     * @param locales
     * @param resultStart
     * @param resultLimit
     * @return
     */
    @Override
    public ResultsWrapper<DataCategory> getDataCategories(boolean locales, int resultStart, int resultLimit) {
        List<DataCategory> activeCategories = new ArrayList<DataCategory>();
        ResultsWrapper<DataCategory> resultsWrapper = dao.getDataCategories(resultStart, resultLimit);
        for (DataCategory dataCategory : resultsWrapper.getResults()) {
            if (dataCategory != null && !dataCategory.isTrash()) {
                activeCategories.add(dataCategory);
            }
        }
        if (locales) {
            localeService.loadLocaleNamesForDataCategories(activeCategories);
        }
        return new ResultsWrapper<DataCategory>(activeCategories, resultsWrapper.isTruncated());
    }

    @Override
    public Map<String, DataCategory> getDataCategoryMap(Set<Long> dataCategoryIds) {
        Map<String, DataCategory> dataCategoryMap = new HashMap<String, DataCategory>();
        for (DataCategory dataCategory : dao.getDataCategories(dataCategoryIds)) {
            if (!dataCategory.isTrash()) {
                dataCategoryMap.put(dataCategory.getUid(), dataCategory);
            }
        }
        return dataCategoryMap;
    }

    @Override
    public List<DataCategory> getDataCategoriesModifiedWithin(Date modifiedSince, Date modifiedUntil) {
        return dao.getDataCategoriesModifiedWithin(modifiedSince, modifiedUntil);
    }

    @Override
    public List<DataCategory> getDataCategoriesForDataItemsModifiedWithin(Date modifiedSince, Date modifiedUntil) {
        return dao.getDataCategoriesForDataItemsModifiedWithin(modifiedSince, modifiedUntil);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Map<String, IDataCategoryReference> getDataCategories(IDataCategoryReference dataCategoryReference) {
        if (log.isDebugEnabled()) {
            log.debug("getDataCategories() " + dataCategoryReference.getFullPath());
        }
        Map<String, IDataCategoryReference> dataCategories =
                (Map<String, IDataCategoryReference>) cacheHelper.getCacheable(new DataCategoryChildrenFactory(dataCategoryReference, dao));
        localeService.loadLocaleNamesForDataCategoryReferences(dataCategories.values());
        return dataCategories;
    }

    /**
     * Get the Set of parent Data Category IDs for the supplied Data Category IDs.
     *
     * @param dataCategoryIds Data Category IDs to find parents for
     * @return Set of parent Data Category IDs
     */
    @Override
    public Set<Long> getParentDataCategoryIds(Set<Long> dataCategoryIds) {
        Set<Long> parentDataCategoryIds = dao.getParentDataCategoryIds(dataCategoryIds);
        if (!parentDataCategoryIds.isEmpty()) {
            parentDataCategoryIds.addAll(getParentDataCategoryIds(parentDataCategoryIds));
        }
        return parentDataCategoryIds;
    }

    @Override
    public Set<AMEEEntityReference> getDataCategoryReferences(ItemDefinition itemDefinition) {
        return dao.getDataCategoryReferences(itemDefinition);
    }

    /**
     * Returns true if the path of the supplied DataCategory is unique amongst peers.
     *
     * @param dataCategory to check for uniqueness
     * @return true if the DataCategory has a unique path amongst peers
     */
    @Override
    public boolean isDataCategoryUniqueByPath(DataCategory dataCategory) {
        return dao.isDataCategoryUniqueByPath(dataCategory);
    }

    /**
     * Returns true if the wikiName of the supplied DataCategory is unique.
     *
     * @param dataCategory to check for uniqueness
     * @return true if the DataCategory has a unique wikiName
     */
    @Override
    public boolean isDataCategoryUniqueByWikiName(DataCategory dataCategory) {
        return dao.isDataCategoryUniqueByWikiName(dataCategory);
    }

    /**
     * Fetch the most recent modified timestamp of all entities the supplied DataCategory relates to. Will check the
     * DataCategory, ItemDefinitions, ItemValueDefinitions, DataItems and ItemValues.
     *
     * @param dataCategory to fetch timestamp for
     * @return the most recent modified timestamp for the DataCategory and related entities
     */
    @Override
    public Date getDataCategoryModifiedDeep(DataCategory dataCategory) {
        // Get the modified dates for all related entities.
        Date dataCategoryModified = dataCategory.getModified();
        Date dataItemsModified = getDataItemsModifiedDeep(dataCategory);
        // Work out which date is the latest.
        Date modified = DataItemService.EPOCH;
        modified = dataCategoryModified.after(modified) ? dataCategoryModified : modified;
        modified = dataItemsModified.after(modified) ? dataItemsModified : modified;
        // Now we have the most recent modified timestamp of all entities related to this DataCategory.
        return modified;
    }

    /**
     * Fetch the most recent modified timestamp of all entities the supplied DataCategory relates to. Will check the
     * ItemDefinitions, ItemValueDefinitions, DataItems.
     *
     * @param dataCategory to fetch timestamp for
     * @return the most recent modified timestamp for the DataCategory and related entities
     */
    @Override
    public Date getDataItemsModifiedDeep(DataCategory dataCategory) {
        // Get the modified dates for all related entities.
        Date dataItemsModified = dataItemService.getDataItemsModified(dataCategory);
        Date definitionsModified =
                dataCategory.isItemDefinitionPresent() ? dataCategory.getItemDefinition().getModifiedDeep() : DataItemService.EPOCH;
        // Work out which date is the latest.
        Date modified = DataItemService.EPOCH;
        modified = dataItemsModified.after(modified) ? dataItemsModified : modified;
        modified = definitionsModified.after(modified) ? definitionsModified : modified;
        // Now we have the most recent modified timestamp of all entities related to this DataCategory.
        return modified;
    }

    @Override
    public void persist(DataCategory dataCategory) {
        dao.persist(dataCategory);
    }

    @Override
    public void remove(DataCategory dataCategory) {
        dao.remove(dataCategory);
    }

    /**
     * Clears all caches related to the supplied DataCategory.
     *
     * @param dataCategory to clear caches for
     */
    @Override
    public void clearCaches(DataCategory dataCategory) {
        log.info("clearCaches() dataCategory: " + dataCategory.getUid());
        drillDownService.clearDrillDownCache();
        dao.invalidate(dataCategory);
        cacheHelper.clearCache("DataCategoryChildren");
        // TODO: Metadata?
        // TODO: Locales?
        // TODO: What else?
    }

    // API Versions

    @Override
    public List<APIVersion> getAPIVersions() {
        return dao.getAPIVersions();
    }

    /**
     * Gets an APIVersion based on the supplied version parameter.
     *
     * @param version to fetch
     * @return APIVersion object, or null
     */
    @Override
    public APIVersion getAPIVersion(String version) {
        return dao.getAPIVersion(version);
    }
}
