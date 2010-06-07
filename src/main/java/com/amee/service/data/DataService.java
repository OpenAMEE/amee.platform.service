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

import com.amee.base.transaction.TransactionController;
import com.amee.base.utils.UidGen;
import com.amee.domain.APIVersion;
import com.amee.domain.ObjectType;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemValue;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.environment.Environment;
import com.amee.domain.sheet.Choice;
import com.amee.domain.sheet.Choices;
import com.amee.domain.sheet.Sheet;
import com.amee.service.BaseService;
import com.amee.service.invalidation.InvalidationMessage;
import com.amee.service.invalidation.InvalidationService;
import com.amee.service.locale.LocaleService;
import com.amee.service.path.PathItemService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private DataSheetService dataSheetService;

    @Autowired
    private PathItemService pathItemService;

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

    public DataCategory getDataCategoryByIdentifier(Environment environment, String identifier) {
        DataCategory dataCategory = null;
        if (UidGen.INSTANCE_12.isValid(identifier)) {
            dataCategory = getDataCategoryByUid(identifier);
        }
        if (dataCategory == null) {
            dataCategory = getDataCategoryByWikiName(environment, identifier);
        }
        return dataCategory;

    }

    public DataCategory getDataCategoryByWikiName(Environment environment, String wikiName) {
        return getDataCategoryByWikiName(environment, wikiName, false);
    }

    public DataCategory getDataCategoryByWikiName(Environment environment, String wikiName, boolean includeTrash) {
        DataCategory dataCategory = dao.getDataCategoryByWikiName(environment, wikiName, includeTrash);
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

    public List<DataCategory> getDataCategories(Environment environment) {
        return getDataCategories(environment, false);
    }

    public List<DataCategory> getDataCategories(Environment environment, boolean locales) {
        List<DataCategory> activeCategories = new ArrayList<DataCategory>();
        for (DataCategory dataCategory : dao.getDataCategories(environment)) {
            if (dataCategory != null && !dataCategory.isTrash()) {
                activeCategories.add(dataCategory);
            }
        }
        if (locales) {
            localeService.loadLocaleNamesForDataCategories(activeCategories);
        }
        return activeCategories;
    }

    public Map<Long, DataCategory> getDataCategoryMap(Environment environment, Set<Long> dataCategoryIds) {
        Map<Long, DataCategory> dataCategoryMap = new HashMap<Long, DataCategory>();
        for (DataCategory dataCategory : dao.getDataCategories(environment, dataCategoryIds)) {
            dataCategoryMap.put(dataCategory.getEntityId(), dataCategory);
        }
        return dataCategoryMap;
    }

    public List<DataCategory> getDataCategories(Environment environment, Set<Long> dataCategoryIds) {
        return dao.getDataCategories(environment, dataCategoryIds);
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
        dataSheetService.removeSheet(dataCategory);
    }

    // DataItems

    public DataItem getDataItem(Environment environment, String path) {
        DataItem dataItem = null;
        if (!StringUtils.isBlank(path)) {
            if (UidGen.INSTANCE_12.isValid(path)) {
                dataItem = getDataItemByUid(environment, path);
            }
            if (dataItem == null) {
                dataItem = getDataItemByPath(environment, path);
            }
        }
        return dataItem;
    }

    public DataItem getDataItemByUid(DataCategory dataCategory, String uid) {
        DataItem dataItem = getDataItemByUid(dataCategory.getEnvironment(), uid);
        if ((dataItem != null) && dataItem.getDataCategory().equals(dataCategory)) {
            return dataItem;
        } else {
            return null;
        }
    }

    private DataItem getDataItemByUid(Environment environment, String uid) {
        DataItem dataItem = dao.getDataItemByUid(uid);
        if ((dataItem != null) && !dataItem.isTrash()) {
            checkEnvironmentObject(environment, dataItem);
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    private DataItem getDataItemByPath(Environment environment, String path) {
        DataItem dataItem = dao.getDataItemByPath(environment, path);
        if ((dataItem != null) && !dataItem.isTrash()) {
            checkEnvironmentObject(environment, dataItem);
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    public Map<Long, DataItem> getDataItemMap(Environment environment, Set<Long> dataItemIds) {
        return getDataItemMap(environment, dataItemIds, false);
    }

    public Map<Long, DataItem> getDataItemMap(Environment environment, Set<Long> dataItemIds, boolean values) {
        Map<Long, DataItem> dataItemMap = new HashMap<Long, DataItem>();
        for (DataItem dataItem : dao.getDataItems(environment, dataItemIds, values)) {
            dataItemMap.put(dataItem.getEntityId(), dataItem);
        }
        return dataItemMap;
    }

    public List<DataItem> getDataItems(Environment environment, Set<Long> dataItemIds) {
        return getDataItems(environment, dataItemIds, false);
    }

    public List<DataItem> getDataItems(Environment environment, Set<Long> dataItemIds, boolean values) {
        return dao.getDataItems(environment, dataItemIds, values);
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

    // Sheets & Choices

    public Sheet getSheet(DataBrowser browser, String fullPath) {
        return dataSheetService.getSheet(browser, fullPath);
    }

    @SuppressWarnings(value = "unchecked")
    public Choices getUserValueChoices(DataItem dataItem, APIVersion apiVersion) {
        List<Choice> userValueChoices = new ArrayList<Choice>();
        for (ItemValueDefinition ivd : dataItem.getItemDefinition().getItemValueDefinitions()) {
            if (ivd.isFromProfile() && ivd.isValidInAPIVersion(apiVersion)) {
                // start default value with value from ItemValueDefinition
                String defaultValue = ivd.getValue();
                // next give DataItem a chance to set the default value, if appropriate
                if (ivd.isFromData()) {
                    ItemValue dataItemValue = dataItem.getItemValue(ivd.getPath());
                    if ((dataItemValue != null) && (dataItemValue.getValue().length() > 0)) {
                        defaultValue = dataItemValue.getValue();
                    }
                }
                // create Choice
                userValueChoices.add(new Choice(ivd.getPath(), defaultValue));
            }
        }
        return new Choices("userValueChoices", userValueChoices);
    }
}
