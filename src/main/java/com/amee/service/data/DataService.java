/**
 * This file is part of AMEE.
 * <p/>
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
package com.amee.service.data;

import com.amee.base.domain.ResultsWrapper;
import com.amee.base.transaction.TransactionController;
import com.amee.base.utils.UidGen;
import com.amee.domain.*;
import com.amee.domain.cache.CacheHelper;
import com.amee.domain.data.*;
import com.amee.domain.item.data.NuDataItem;
import com.amee.service.BaseService;
import com.amee.service.invalidation.InvalidationMessage;
import com.amee.service.invalidation.InvalidationService;
import com.amee.service.item.DataItemService;
import com.amee.service.locale.LocaleService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Primary service interface to Data Resources.
 */
@Service
public class DataService extends BaseService implements IDataService, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private InvalidationService invalidationService;

    @Autowired
    private DataServiceDAO dao;

    @Autowired
    private DrillDownService drillDownService;

    @Autowired
    private LocaleService localeService;

    @Autowired
    private DataItemService dataItemService;

    private CacheHelper cacheHelper = CacheHelper.getInstance();

    // Events

    public void onApplicationEvent(ApplicationEvent event) {
        if (InvalidationMessage.class.isAssignableFrom(event.getClass())) {
            onInvalidationMessage((InvalidationMessage) event);
        }
    }

    @Transactional(readOnly = true)
    private void onInvalidationMessage(InvalidationMessage invalidationMessage) {
        if ((invalidationMessage.isLocal() || invalidationMessage.isFromOtherInstance()) &&
                invalidationMessage.getObjectType().equals(ObjectType.DC)) {
            log.debug("onInvalidationMessage() Handling InvalidationMessage.");
            DataCategory dataCategory = getDataCategoryByUid(invalidationMessage.getEntityUid(), null);
            if (dataCategory != null) {
                clearCaches(dataCategory);
            }
        }
    }

    // DataCategories

    public DataCategory getRootDataCategory() {
        return dao.getRootDataCategory();
    }

    public IDataCategoryReference getDataCategoryByPath(IDataCategoryReference parent, String path) {
        return getDataCategories(parent).get(path);
    }

    public DataCategory getDataCategoryByIdentifier(String identifier) {
        return getDataCategoryByIdentifier(identifier, AMEEStatus.ACTIVE);
    }

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

    public DataCategory getDataCategoryByWikiName(String wikiName) {
        return getDataCategoryByWikiName(wikiName, AMEEStatus.ACTIVE);
    }

    public DataCategory getDataCategoryByWikiName(String wikiName, AMEEStatus status) {
        return dao.getDataCategoryWithStatus(dao.getDataCategoryByWikiName(wikiName, status), status);
    }

    public DataCategory getDataCategoryByUid(String uid) {
        return dao.getDataCategoryByUidWithActiveStatus(uid);
    }

    public DataCategory getDataCategoryByUid(String uid, AMEEStatus status) {
        return dao.getDataCategoryByUidWithStatus(uid, status);
    }

    /**
     * Get full DataCategory entity based on the supplied IDataCategoryReference.
     *
     * @param dataCategory IDataCategoryReference to fetch a DataCategory for
     * @return the DataCategory matching the IDataCategoryReference
     */
    public DataCategory getDataCategory(IDataCategoryReference dataCategory) {
        return dao.getDataCategory(dataCategory);
    }

    public IDataCategoryReference getDataCategoryByFullPath(String path) {
        IDataCategoryReference dataCategory = null;
        if (!StringUtils.isBlank(path)) {
            dataCategory = getDataCategoryByFullPath(new ArrayList<String>(Arrays.asList(path.split("/"))));
        }
        return dataCategory;
    }

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

    public List<DataCategory> getDataCategories() {
        return getDataCategories(false);
    }

    public ResultsWrapper<DataCategory> getDataCategories(int resultStart, int resultLimit) {
        return getDataCategories(false, resultStart, resultLimit);
    }

    public List<DataCategory> getDataCategories(boolean locales) {
        return getDataCategories(locales, 0, 0).getResults();
    }

    /**
     * TODO: There is a potential for confusing pagination truncation when removing inactive entries.
     *
     * @param locales
     * @param resultStart
     * @param resultLimit
     * @return
     */
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

    public Map<String, DataCategory> getDataCategoryMap(Set<Long> dataCategoryIds) {
        Map<String, DataCategory> dataCategoryMap = new HashMap<String, DataCategory>();
        for (DataCategory dataCategory : dao.getDataCategories(dataCategoryIds)) {
            dataCategoryMap.put(dataCategory.getUid(), dataCategory);
        }
        return dataCategoryMap;
    }

    public List<DataCategory> getDataCategories(Set<Long> dataCategoryIds) {
        return dao.getDataCategories(dataCategoryIds);
    }

    public List<DataCategory> getDataCategoriesModifiedWithin(Date modifiedSince, Date modifiedUntil) {
        return dao.getDataCategoriesModifiedWithin(modifiedSince, modifiedUntil);
    }

    public List<DataCategory> getDataCategoriesForDataItemsModifiedWithin(Date modifiedSince, Date modifiedUntil) {
        return dao.getDataCategoriesForDataItemsModifiedWithin(modifiedSince, modifiedUntil);
    }

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
    public Set<Long> getParentDataCategoryIds(Set<Long> dataCategoryIds) {
        Set<Long> parentDataCategoryIds = dao.getParentDataCategoryIds(dataCategoryIds);
        if (!parentDataCategoryIds.isEmpty()) {
            parentDataCategoryIds.addAll(getParentDataCategoryIds(parentDataCategoryIds));
        }
        return parentDataCategoryIds;
    }

    public Set<AMEEEntityReference> getDataCategoryReferences(ItemDefinition itemDefinition) {
        return dao.getDataCategoryReferences(itemDefinition);
    }

    /**
     * Returns true if the path of the supplied DataCategory is unique amongst peers.
     *
     * @param dataCategory to check for uniqueness
     * @return true if the DataCategory has a unique path amongst peers
     */
    public boolean isDataCategoryUniqueByPath(DataCategory dataCategory) {
        return dao.isDataCategoryUniqueByPath(dataCategory);
    }

    /**
     * Returns true if the wikiName of the supplied DataCategory is unique.
     *
     * @param dataCategory to check for uniqueness
     * @return true if the DataCategory has a unique wikiName
     */
    public boolean isDataCategoryUniqueByWikiName(DataCategory dataCategory) {
        return dao.isDataCategoryUniqueByWikiName(dataCategory);
    }

    public void persist(DataCategory dataCategory) {
        dao.persist(dataCategory);
    }

    public void remove(DataCategory dataCategory) {
        dao.remove(dataCategory);
    }

    /**
     * Invalidate a DataCategory. This will send an invalidation message via the
     * InvalidationService and clear the local caches.
     *
     * @param dataCategory to invalidate
     */
    public void invalidate(DataCategory dataCategory) {
        log.info("invalidate() dataCategory: " + dataCategory.getUid());
        invalidationService.add(dataCategory);
    }

    /**
     * Invalidate a DataCategory fully. This will send an invalidation message via the
     * InvalidationService and clear the local caches. It was also trigger a re-index of the Data Items.
     *
     * @param dataCategory to invalidate
     */
    public void invalidateFull(DataCategory dataCategory) {
        log.info("invalidate() dataCategory: " + dataCategory.getUid());
        invalidationService.add(dataCategory, "indexDataItems");
    }

    /**
     * Clears all caches related to the supplied DataCategory.
     *
     * @param dataCategory to clear caches for
     */
    public void clearCaches(DataCategory dataCategory) {
        log.info("clearCaches() dataCategory: " + dataCategory.getUid());
        drillDownService.clearDrillDownCache();
        dao.invalidate(dataCategory);
        cacheHelper.clearCache("DataCategoryChildren");
        // TODO: Metadata?
        // TODO: Locales?
        // TODO: What else?
    }

    // DataItems

    public DataItem getDataItemByIdentifier(DataCategory parent, String path) {
        DataItem dataItem = null;
        if (!StringUtils.isBlank(path)) {
            if (UidGen.INSTANCE_12.isValid(path)) {
                dataItem = getDataItemByUid(parent, path);
            }
            if (dataItem == null) {
                dataItem = getDataItemByPath(parent, path);
            }
        }
        return dataItem;
    }

    public DataItem getDataItemByUid(DataCategory parent, String uid) {
        DataItem dataItem = getDataItemByUid(uid);
        if ((dataItem != null) && dataItem.getDataCategory().equals(parent)) {
            return dataItem;
        } else {
            return null;
        }
    }

    public DataItem getDataItemByUid(String uid) {
        DataItem dataItem = DataItem.getDataItem(dataItemService.getItemByUid(uid));
        if (dataItem == null) {
            dataItem = dao.getDataItemByUid(uid);
        }
        if ((dataItem != null) && !dataItem.isTrash()) {
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    public DataItem getDataItemByPath(DataCategory parent, String path) {
        DataItem dataItem = DataItem.getDataItem(dataItemService.getDataItemByPath(parent, path));
        if (dataItem == null) {
            dataItem = dao.getDataItemByPath(parent, path);
        }
        if ((dataItem != null) && !dataItem.isTrash()) {
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    public Map<String, DataItem> getDataItemMap(Set<Long> dataItemIds, boolean loadValues) {
        Map<String, DataItem> dataItemMap = new HashMap<String, DataItem>();
        for (DataItem dataItem : dao.getDataItems(dataItemIds, loadValues)) {
            dataItemMap.put(dataItem.getUid(), dataItem);
        }
        localeService.loadLocaleNamesForDataItems(dataItemMap.values(), true);
        return dataItemMap;
    }

    public List<DataItem> getDataItems(Set<Long> dataItemIds) {
        return getDataItems(dataItemIds, false);
    }

    public List<DataItem> getDataItems(Set<Long> dataItemIds, boolean loadValues) {
        List<DataItem> dataItems = new ArrayList<DataItem>();
        for (DataItem dataItem : dao.getDataItems(dataItemIds, loadValues)) {
            dataItems.add(dataItem);
        }
        localeService.loadLocaleNamesForDataItems(dataItems, true);
        return dataItems;
    }

    public List<DataItem> getDataItems(IDataCategoryReference dataCategory) {
        return getDataItems(dataCategory, true);
    }

    public List<DataItem> getDataItems(IDataCategoryReference dataCategory, boolean checkDataItems) {
        Set<String> dataItemUids = new HashSet<String>();
        List<DataItem> dataItems = new ArrayList<DataItem>();
        for (NuDataItem nuDataItem : dataItemService.getDataItems(dataCategory)) {
            dataItemUids.add(nuDataItem.getUid());
            dataItems.add(DataItem.getDataItem(nuDataItem));
        }
        for (DataItem dataItem : dao.getDataItems(dataCategory)) {
            if (!dataItemUids.contains(dataItem.getUid())) {
                dataItems.add(dataItem);
            }
        }
        localeService.loadLocaleNamesForDataItems(dataItems, true);
        return activeDataItems(dataItems, checkDataItems);
    }

    private List<DataItem> activeDataItems(List<DataItem> dataItems) {
        return activeDataItems(dataItems, true);
    }

    private List<DataItem> activeDataItems(List<DataItem> dataItems, boolean checkDataItems) {
        List<DataItem> activeDataItems = new ArrayList<DataItem>();
        for (DataItem dataItem : dataItems) {
            if (!dataItem.isTrash()) {
                if (checkDataItems) {
                    checkDataItem(dataItem);
                }
                activeDataItems.add(dataItem);
            }
        }
        return activeDataItems;
    }

    /**
     * Add to the {@link com.amee.domain.data.DataItem} any {@link com.amee.domain.data.ItemValue}s it is missing.
     * This will be the case on first persist (this method acting as a reification function), and between GETs if any
     * new {@link com.amee.domain.data.ItemValueDefinition}s have been added to the underlying
     * {@link com.amee.domain.data.ItemDefinition}.
     * <p/>
     * Any updates to the {@link com.amee.domain.data.DataItem} will be persisted to the database.
     *
     * @param dataItem - the DataItem to check
     */
    @SuppressWarnings(value = "unchecked")
    public void checkDataItem(DataItem dataItem) {

        if (dataItem == null) {
            return;
        }

        Set<ItemValueDefinition> existingItemValueDefinitions = dataItem.getItemValueDefinitions();
        Set<ItemValueDefinition> missingItemValueDefinitions = new HashSet<ItemValueDefinition>();

        // find ItemValueDefinitions not currently implemented in this Item
        for (ItemValueDefinition ivd : dataItem.getItemDefinition().getItemValueDefinitions()) {
            if (ivd.isFromData()) {
                if (!existingItemValueDefinitions.contains(ivd)) {
                    missingItemValueDefinitions.add(ivd);
                }
            }
        }

        // Do we need to add any ItemValueDefinitions?
        if (missingItemValueDefinitions.size() > 0) {

            // Ensure a transaction has been opened. The implementation of open-session-in-view we are using
            // does not open transactions for GETs. This method is called for certain GETs.
            transactionController.begin(true);

            // create missing ItemValues
            for (ItemValueDefinition ivd : missingItemValueDefinitions) {
                persist(new ItemValue(ivd, dataItem, ""));
            }

            // clear caches
            dataItemService.clearItemValues();
            invalidate(dataItem.getDataCategory());
        }
    }

    public void persist(DataItem dataItem) {
        persist(dataItem, true);
    }

    public void persist(DataItem dataItem, boolean checkDataItem) {
        if (dataItem.isLegacy()) {
            dao.persist(dataItem);
        } else {
            dataItemService.persist(dataItem.getNuEntity());
        }
        if (checkDataItem) {
            checkDataItem(dataItem);
        }
    }

    public void remove(DataItem dataItem) {
        if (dataItem.isLegacy()) {
            dao.remove(dataItem);
        } else {
            dataItemService.remove(dataItem);
        }
    }

    // ItemValues.

    public void persist(ItemValue itemValue) {
        if (!itemValue.isLegacy()) {
            dataItemService.persist(itemValue.getNuEntity());
        }
    }

    public void remove(ItemValue itemValue) {
        if (itemValue.isLegacy()) {
            dao.remove(itemValue);
        } else {
            dataItemService.remove(itemValue.getNuEntity());
        }
    }

    // API Versions

    public List<APIVersion> getAPIVersions() {
        return dao.getAPIVersions();
    }

    /**
     * Gets an APIVersion based on the supplied version parameter.
     *
     * @param version to fetch
     * @return APIVersion object, or null
     */
    public APIVersion getAPIVersion(String version) {
        return dao.getAPIVersion(version);
    }
}
