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
import com.amee.domain.AMEEEntityReference;
import com.amee.domain.APIVersion;
import com.amee.domain.ObjectType;
import com.amee.domain.data.*;
import com.amee.service.BaseService;
import com.amee.service.invalidation.InvalidationMessage;
import com.amee.service.invalidation.InvalidationService;
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
public class DataService extends BaseService implements ApplicationListener {

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
            DataCategory dataCategory = getDataCategoryByUid(invalidationMessage.getEntityUid(), true);
            if (dataCategory != null) {
                clearCaches(dataCategory);
            }
        }
    }

    // DataCategories

    public DataCategory getRootDataCategory() {
        return dao.getRootDataCategory();
    }

    public DataCategory getDataCategoryByIdentifier(String identifier) {
        DataCategory dataCategory = null;
        if (UidGen.INSTANCE_12.isValid(identifier)) {
            dataCategory = getDataCategoryByUid(identifier);
        }
        if (dataCategory == null) {
            dataCategory = getDataCategoryByWikiName(identifier);
        }
        return dataCategory;

    }

    public DataCategory getDataCategoryByWikiName(String wikiName) {
        return getDataCategoryByWikiName(wikiName, false);
    }

    public DataCategory getDataCategoryByWikiName(String wikiName, boolean includeTrash) {
        DataCategory dataCategory = dao.getDataCategoryByWikiName(wikiName, includeTrash);
        if ((dataCategory != null) && (includeTrash || !dataCategory.isTrash())) {
            return dataCategory;
        } else {
            return null;
        }
    }

    public DataCategory getDataCategoryByUid(String uid) {
        return getDataCategoryByUid(uid, false);
    }

    public DataCategory getDataCategoryByUid(String uid, boolean includeTrash) {
        DataCategory dataCategory = dao.getDataCategoryByUid(uid, includeTrash);
        if ((dataCategory != null) && (includeTrash || !dataCategory.isTrash())) {
            return dataCategory;
        } else {
            return null;
        }
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

    public Map<Long, DataCategory> getDataCategoryMap(Set<Long> dataCategoryIds) {
        Map<Long, DataCategory> dataCategoryMap = new HashMap<Long, DataCategory>();
        for (DataCategory dataCategory : dao.getDataCategories(dataCategoryIds)) {
            dataCategoryMap.put(dataCategory.getEntityId(), dataCategory);
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

    public List<DataCategory> getDataCategories(DataCategory dataCategory) {
        return dao.getDataCategories(dataCategory);
    }

    public boolean hasDataCategories(DataCategory dataCategory, Collection<Long> dataCategoryIds) {
        log.debug("hasDataCategories() " + dataCategory.toString());
        if (dataCategoryIds.contains(dataCategory.getId())) {
            return true;
        }
        for (DataCategory dc : getDataCategories(dataCategory)) {
            if (dataCategoryIds.contains(dc.getId())) {
                return true;
            }
            if (hasDataCategories(dc, dataCategoryIds)) {
                return true;
            }
        }
        return false;
    }

    public Set<AMEEEntityReference> getDataCategoryReferences(ItemDefinition itemDefinition) {
        return dao.getDataCategoryReferences(itemDefinition);
    }

    public boolean isUnique(DataCategory dataCategory) {
        return dao.isUnique(dataCategory);
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
     * Clears all caches related to the supplied DataCategory.
     *
     * @param dataCategory to clear caches for
     */
    public void clearCaches(DataCategory dataCategory) {
        log.info("clearCaches() dataCategory: " + dataCategory.getUid());
        drillDownService.clearDrillDownCache();
        dao.invalidate(dataCategory);
        // TODO: Metadata?
        // TODO: Locales?
        // TODO: What else?
    }

    // DataItems

    public DataItem getDataItem(String path) {
        DataItem dataItem = null;
        if (!StringUtils.isBlank(path)) {
            if (UidGen.INSTANCE_12.isValid(path)) {
                dataItem = getDataItemByUid(path);
            }
            if (dataItem == null) {
                dataItem = getDataItemByPath(path);
            }
        }
        return dataItem;
    }

    public DataItem getDataItemByUid(DataCategory dataCategory, String uid) {
        DataItem dataItem = getDataItemByUid(uid);
        if ((dataItem != null) && dataItem.getDataCategory().equals(dataCategory)) {
            return dataItem;
        } else {
            return null;
        }
    }

    private DataItem getDataItemByUid(String uid) {
        DataItem dataItem = dao.getDataItemByUid(uid);
        if ((dataItem != null) && !dataItem.isTrash()) {
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    private DataItem getDataItemByPath(String path) {
        DataItem dataItem = dao.getDataItemByPath(path);
        if ((dataItem != null) && !dataItem.isTrash()) {
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    public Map<Long, DataItem> getDataItemMap(Set<Long> dataItemIds) {
        return getDataItemMap(dataItemIds, false);
    }

    public Map<Long, DataItem> getDataItemMap(Set<Long> dataItemIds, boolean values) {
        Map<Long, DataItem> dataItemMap = new HashMap<Long, DataItem>();
        for (DataItem dataItem : dao.getDataItems(dataItemIds, values)) {
            dataItemMap.put(dataItem.getEntityId(), dataItem);
        }
        return dataItemMap;
    }

    public List<DataItem> getDataItems(Set<Long> dataItemIds) {
        return getDataItems(dataItemIds, false);
    }

    public List<DataItem> getDataItems(Set<Long> dataItemIds, boolean values) {
        return dao.getDataItems(dataItemIds, values);
    }

    public List<DataItem> getDataItems(DataCategory dataCategory) {
        return getDataItems(dataCategory, true);
    }

    public List<DataItem> getDataItems(DataCategory dataCategory, boolean checkDataItems) {
        return activeDataItems(dao.getDataItems(dataCategory), checkDataItems);
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
                new ItemValue(ivd, dataItem, "");
            }

            // clear caches
            invalidate(dataItem.getDataCategory());
        }
    }

    public void persist(DataItem dataItem) {
        dao.persist(dataItem);
        checkDataItem(dataItem);
    }

    public void remove(DataItem dataItem) {
        dao.remove(dataItem);
    }

    public void remove(ItemValue dataItemValue) {
        dao.remove(dataItemValue);
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
