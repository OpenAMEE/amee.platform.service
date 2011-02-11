package com.amee.service.item;

import com.amee.base.transaction.TransactionController;
import com.amee.base.utils.UidGen;
import com.amee.domain.*;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.BaseDataItemValue;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.item.data.DataItemNumberValue;
import com.amee.domain.item.data.DataItemTextValue;
import com.amee.domain.sheet.Choice;
import com.amee.domain.sheet.Choices;
import com.amee.platform.science.StartEndDate;
import com.amee.service.data.DataService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataItemService extends ItemService implements IDataItemService {

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private DataService dataService;

    @Autowired
    private DataItemServiceDAO dao;

    @Override
    public List<DataItem> getDataItems(IDataCategoryReference dataCategory) {
        return getDataItems(dataCategory, true);
    }

    @Override
    public List<DataItem> getDataItems(IDataCategoryReference dataCategory, boolean checkDataItems) {
        List<DataItem> dataItems = new ArrayList<DataItem>();
        for (DataItem dataItem : dao.getDataItems(dataCategory)) {
            dataItems.add(dataItem);
        }
        return activeDataItems(dataItems, checkDataItems);
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
        loadItemValuesForItems((List) activeDataItems);
        localeService.loadLocaleNamesForDataItems(activeDataItems);
        return activeDataItems;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataItem> getDataItems(Set<Long> dataItemIds) {
        List<DataItem> dataItems = dao.getDataItems(dataItemIds);
        loadItemValuesForItems((List) dataItems);
        localeService.loadLocaleNamesForDataItems(dataItems);
        return dataItems;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Map<String, DataItem> getDataItemMap(Set<Long> dataItemIds, boolean loadValues) {
        Map<String, DataItem> dataItemMap = new HashMap<String, DataItem>();
        Set<BaseItemValue> dataItemValues = new HashSet<BaseItemValue>();
        // Load all DataItems and BaseItemValues, if required.
        List<DataItem> dataItems = dao.getDataItems(dataItemIds);
        if (loadValues) {
            loadItemValuesForItems((List) dataItems);
        }
        // Add DataItems to map. Add BaseItemValue, if required.
        for (DataItem dataItem : dataItems) {
            dataItemMap.put(dataItem.getUid(), dataItem);
            if (loadValues) {
                dataItemValues.addAll(this.getItemValues(dataItem));
            }
        }
        localeService.loadLocaleNamesForDataItems(dataItemMap.values(), dataItemValues);
        return dataItemMap;
    }

    @Override
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

    @Override
    public DataItem getDataItemByUid(DataCategory parent, String uid) {
        DataItem dataItem = getItemByUid(uid);
        if ((dataItem != null) && dataItem.getDataCategory().equals(parent)) {
            return dataItem;
        } else {
            return null;
        }
    }

    @Override
    public DataItem getItemByUid(String uid) {
        DataItem dataItem = dao.getItemByUid(uid);
        if ((dataItem != null) && (!dataItem.isTrash())) {
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    @Override
    public DataItem getDataItemByPath(DataCategory parent, String path) {
        DataItem dataItem = dao.getDataItemByPath(parent, path);
        if ((dataItem != null) && !dataItem.isTrash()) {
            checkDataItem(dataItem);
            return dataItem;
        } else {
            return null;
        }
    }

    @Override
    public String getLabel(DataItem dataItem) {
        String label = "";
        BaseItemValue itemValue;
        ItemDefinition itemDefinition = dataItem.getItemDefinition();
        for (Choice choice : itemDefinition.getDrillDownChoices()) {
            itemValue = getItemValue(dataItem, choice.getName());
            if ((itemValue != null) &&
                    (itemValue.getValueAsString().length() > 0) &&
                    !itemValue.getValueAsString().equals("-")) {
                if (label.length() > 0) {
                    label = label.concat(", ");
                }
                label = label.concat(itemValue.getValueAsString());
            }
        }
        if (label.length() == 0) {
            label = dataItem.getDisplayPath();
        }
        return label;
    }

    @Override
    public Choices getUserValueChoices(DataItem dataItem, APIVersion apiVersion) {
        List<Choice> userValueChoices = new ArrayList<Choice>();
        for (ItemValueDefinition ivd : dataItem.getItemDefinition().getItemValueDefinitions()) {
            if (ivd.isFromProfile() && ivd.isValidInAPIVersion(apiVersion)) {
                // start default value with value from ItemValueDefinition
                String defaultValue = ivd.getValue();
                // next give DataItem a chance to set the default value, if appropriate
                if (ivd.isFromData()) {
                    BaseItemValue dataItemValue = getItemValue(dataItem, ivd.getPath());
                    if ((dataItemValue != null) && (dataItemValue.getValueAsString().length() > 0)) {
                        defaultValue = dataItemValue.getValueAsString();
                    }
                }
                // create Choice
                userValueChoices.add(new Choice(ivd.getPath(), defaultValue));
            }
        }
        return new Choices("userValueChoices", userValueChoices);
    }

    /**
     * Add to the {@link com.amee.domain.item.data.DataItem} any {@link com.amee.domain.item.data.BaseDataItemValue}s it is missing.
     * This will be the case on first persist (this method acting as a reification function), and between GETs if any
     * new {@link com.amee.domain.data.ItemValueDefinition}s have been added to the underlying
     * {@link com.amee.domain.data.ItemDefinition}.
     * <p/>
     * Any updates to the {@link com.amee.domain.item.data.DataItem} will be persisted to the database.
     *
     * @param dataItem - the DataItem to check
     */
    @SuppressWarnings(value = "unchecked")
    public void checkDataItem(DataItem dataItem) {

        if (dataItem == null) {
            return;
        }

        Set<ItemValueDefinition> existingItemValueDefinitions = getItemValueDefinitionsInUse(dataItem);
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
                BaseDataItemValue itemValue;
                // Create a value.
                if (ivd.getValueDefinition().getValueType().equals(ValueType.INTEGER) ||
                        ivd.getValueDefinition().getValueType().equals(ValueType.DOUBLE)) {
                    // Item is a number.
                    itemValue = new DataItemNumberValue(ivd, dataItem);
                } else {
                    // Item is text.
                    itemValue = new DataItemTextValue(ivd, dataItem, "");
                }
                persist(itemValue);
            }

            // clear caches
            clearItemValues();
            dataService.invalidate(dataItem.getDataCategory());
        }
    }

    @Override
    public void remove(DataItem dataItem) {
        dataItem.setStatus(AMEEStatus.TRASH);
    }

    @Override
    public void persist(DataItem dataItem) {
        persist(dataItem, true);
    }

    @Override
    public void persist(DataItem dataItem, boolean checkDataItem) {
        dao.persist(dataItem);
        if (checkDataItem) {
            checkDataItem(dataItem);
        }
    }

    // ItemValues.

    /**
     * Get an {@link BaseItemValue} belonging to this Item using some identifier and prevailing datetime context.
     *
     * @param identifier - a value to be compared to the path and then the uid of the Item Values belonging
     *                   to this Item.
     * @return the matched {@link BaseItemValue} or NULL if no match is found.
     */
    @Override
    public BaseItemValue getItemValue(BaseItem item, String identifier) {
        if (!DataItem.class.isAssignableFrom(item.getClass()))
            throw new IllegalStateException("A DataItem instance was expected.");
        return getItemValue(item, identifier, item.getEffectiveStartDate());
    }

    @Override
    public void persist(BaseItemValue itemValue) {
        dao.persist(itemValue);
    }

    @Override
    public void remove(BaseItemValue itemValue) {
        itemValue.setStatus(AMEEStatus.TRASH);
    }

    @Override
    public StartEndDate getStartDate(DataItem dataItem) {
        return null;
    }

    @Override
    public StartEndDate getEndDate(DataItem dataItem) {
        return null;
    }

    @Override
    protected DataItemServiceDAO getDao() {
        return dao;
    }
}
