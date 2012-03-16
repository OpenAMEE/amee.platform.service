package com.amee.service.item;

import com.amee.base.domain.ResultsWrapper;
import com.amee.base.transaction.TransactionController;
import com.amee.base.utils.UidGen;
import com.amee.domain.AMEEStatus;
import com.amee.domain.APIVersion;
import com.amee.domain.DataItemService;
import com.amee.domain.DataItemValuesFilter;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.ValueType;
import com.amee.domain.data.BaseItemValueStartDateComparator;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.data.ItemValueMap;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.HistoryValue;
import com.amee.domain.item.data.BaseDataItemValue;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.item.data.DataItemNumberValue;
import com.amee.domain.item.data.DataItemTextValue;
import com.amee.domain.sheet.Choice;
import com.amee.domain.sheet.Choices;
import com.amee.platform.science.ExternalHistoryValue;
import com.amee.platform.science.ExternalNumberValue;
import com.amee.platform.science.StartEndDate;
import com.amee.service.data.DrillDownService;
import com.amee.service.invalidation.InvalidationService;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.log4j.Log4JStopWatch;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataItemServiceImpl extends AbstractItemService implements DataItemService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private InvalidationService invalidationService;

    @Autowired
    private DrillDownService drillDownService;

    @Autowired
    private DataItemServiceDAO dao;

    @Override
    public long getDataItemCount(IDataCategoryReference dataCategory) {
        return dao.getDataItemCount(dataCategory);
    }

    @Override
    public List<DataItem> getDataItems(IDataCategoryReference dataCategory) {
        return getDataItems(dataCategory, true);
    }

    @Override
    public List<DataItem> getDataItems(IDataCategoryReference dataCategory, boolean checkDataItems) {
        return activeDataItems(dao.getDataItems(dataCategory), checkDataItems, true);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataItem> getDataItems(Set<Long> dataItemIds) {
        return activeDataItems(dao.getDataItems(dataItemIds), false, true);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Map<String, DataItem> getDataItemMap(Set<Long> dataItemIds, boolean loadValues) {
        Map<String, DataItem> dataItemMap = new HashMap<String, DataItem>();
        Set<BaseItemValue> dataItemValues = new HashSet<BaseItemValue>();
        // Load all DataItems and BaseItemValues, if required.
        List<DataItem> dataItems = activeDataItems(dao.getDataItems(dataItemIds), false, loadValues);
        // Add DataItems to map. Add BaseItemValue, if required.
        for (DataItem dataItem : dataItems) {
            dataItemMap.put(dataItem.getUid(), dataItem);
            if (loadValues) {
                dataItemValues.addAll(getItemValues(dataItem));
            }
        }
        localeService.loadLocaleNamesForDataItems(dataItemMap.values(), dataItemValues);
        return dataItemMap;
    }

    private List<DataItem> activeDataItems(List<DataItem> dataItems, boolean checkDataItems, boolean loadValues) {
        List<DataItem> activeDataItems = new ArrayList<DataItem>();
        for (DataItem dataItem : dataItems) {
            if (!dataItem.isTrash()) {
                if (checkDataItems) {
                    checkDataItem(dataItem);
                }
                activeDataItems.add(dataItem);
            }
        }
        if (loadValues) {
            loadItemValuesForItems((List) activeDataItems);
        }
        localeService.loadLocaleNamesForDataItems(activeDataItems);
        return activeDataItems;
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
    public DataItem getDataItemByCategoryAndDrillDowns(DataCategory parent, List<Choice> selections) {
        DataItem dataItem = null;
        Choices choices = drillDownService.getChoices(parent, selections);
        if (choices.getName().equals("uid") && (choices.getChoices().size() > 0)) {
            dataItem = getDataItemByUid(parent, choices.getChoices().get(0).getValue());
        } else {

        // The drill down selections didn't produce a data item UID.
        log.debug("getDataItemByCategoryAndDrillDowns() did not find data item for category with UID: " +
            parent.getUid() + " and drillDowns: " + selections);
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

    /**
     * Get a Choices object containing all the item values that may be set by the user (profile).
     * If an item value has a default value in the item value definition this will be used.
     * If an item value has a default value in the data item value this will override the item value definition default.
     *
     * @param dataItem the DataItem to get the List of choices for.
     * @param apiVersion Item Value Definitions are valid for a particular APIVersion.
     * @return A Choices object with the user settable item values.
     */
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
    @Override
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
            invalidationService.add(dataItem.getDataCategory());
        }
    }

    /**
     * Returns the most recent modified timestamp of DataItems for the supplied DataCategory. Will return the
     * minimum supported date if there are no matching DataItems.
     *
     * @param dataCategory to get modified timestamp for
     * @return most recent modified timestamp or the epoch value if not available.
     */
    @Override
    public Date getDataItemsModified(DataCategory dataCategory) {
        Date modified = dao.getDataItemsModified(dataCategory);
        if (modified == null) {
            modified = DataItemService.MYSQL_MIN_DATETIME;
        }
        return modified;
    }

    /**
     * Checks if a DataItem already exists with the same drill down values.
     * NB: This method uses the transient values returned from com.amee.domain.item.data.DataItem#getValues()
     *
     * @param dataItem the DataItem to check for equivalents.
     * @return false if a DataItem already exists with the same category and drill down values. Otherwise, true.
     */
    @Override
    public boolean isUnique(DataItem dataItem) {
        return !equivalentDataItemExists(dataItem);
    }

    private boolean equivalentDataItemExists(DataItem dataItem) {
        Log4JStopWatch stopWatch = new Log4JStopWatch("equivalentDataItemExists");

        // Get a list of this data item's values for the drill downs.
        // The values for these values are null :-(
        List<String> drillDownPaths = getDrillDownPaths(dataItem);

        // Check the drilldown values for all existing data items for the same category.
        // TODO: Should we just try fetching a data item by drilldown values? com.amee.domain.DataItemService.getDataItemByCategoryAndDrillDowns()
        for (DataItem existingDataItem : getDataItems(dataItem.getDataCategory(), false)) {

            // Ignore the one we just added. This is the one we are checking!
            if (existingDataItem.getUid().equals(dataItem.getUid())) {
                continue;
            }

            // Must have the same item definition to be considered a dupe.
            if (existingDataItem.getItemDefinition().equals(dataItem.getItemDefinition())) {

                // check if it has the same values for the drillDowns we have
                // Create maps of new and existing values
                Map<String, String> newValues = new HashMap<String, String>();
                Map<String, String> existingValues = new HashMap<String, String>();
                for (String path : drillDownPaths) {
                    String newValue = null;

                    // Use reflection to get the values. See: com.amee.domain.item.data.DataItem#getValues().
                    // This is only for v3
                    try {
                        String pathMethod = "get" + StringUtils.capitalize(path);
                        Method getter = dataItem.getValues().getClass().getMethod(pathMethod);
                        newValue = String.valueOf(getter.invoke(dataItem.getValues()));
                    } catch (Exception e) {
                        throw new RuntimeException("equivalentDataItemExists() caught Exception: " + e.getMessage(), e);
                    }

                    // Handle v2
                    if (newValue.equals("null")) {
                        newValue = getItemValuesMap(dataItem).get(path).getValueAsString();
                    }

                    newValues.put(path, newValue);

                    String existingValue = getItemValue(existingDataItem, path).getValueAsString();
                    existingValues.put(path, existingValue);
                }
                if (newValues.equals(existingValues)) {
                    log.info("equivalentDataItemExists() found duplicate data item");
                    stopWatch.stop();
                    return true;
                }
            }
        }
        stopWatch.stop();
        return false;
    }

    /**
     * Returns true if the path of the supplied DataItem is unique amongst peers with the same
     * DataCategory. Empty paths are always treated as 'unique'.
     *
     * @param dataItem to check for uniqueness
     * @return true if the DataItem has a unique path amongst peers or the path is simply empty
     */
    @Override
    public boolean isDataItemUniqueByPath(DataItem dataItem) {
        return dataItem.getPath().isEmpty() || dao.isDataItemUniqueByPath(dataItem);
    }

    /**
     * Returns true if the {@link BaseDataItemValue} supplied has the same startDate as another
     * peer within the same {@link DataItem}.
     * <p/>
     * TODO: This method is not designed for large amounts of DIVHs.
     * TODO: See https://jira.amee.com/browse/PL-2685.
     *
     * @param itemValue {@link BaseDataItemValue} to check
     * @return true if the {@link BaseDataItemValue} supplied has the same startDate as another {@link DataItem}
     */
    @Override
    public boolean isDataItemValueUniqueByStartDate(BaseDataItemValue itemValue) {
        if (HistoryValue.class.isAssignableFrom(itemValue.getClass())) {
            HistoryValue historyValue = (HistoryValue) itemValue;
            for (BaseItemValue existingItemValue : getActiveItemValues(itemValue.getDataItem())) {
                if (existingItemValue.getItemValueDefinition().equals(itemValue.getItemValueDefinition()) &&
                    HistoryValue.class.isAssignableFrom(existingItemValue.getClass())) {
                    HistoryValue existingHistoryValue = (HistoryValue) existingItemValue;
                    if (!historyValue.equals(existingHistoryValue) && historyValue.getStartDate().equals(existingHistoryValue.getStartDate())) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            throw new IllegalStateException("Should not be checking a non-historical DataItemValue.");
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
     * Gets a {@link ResultsWrapper} of {@link BaseDataItemValue}s matching the supplied {@link DataItemValuesFilter}.
     * <p/>
     * TODO: This method is not designed for large amounts of DIVHs.
     * TODO: See https://jira.amee.com/browse/PL-2685.
     *
     * @param filter a {@link DataItemValuesFilter} to match {@link BaseDataItemValue}s against
     * @return a a {@link ResultsWrapper} of {@link BaseDataItemValue}s
     */
    @Override
    public ResultsWrapper<BaseDataItemValue> getAllItemValues(DataItemValuesFilter filter) {

        boolean handledFirst = false;
        boolean truncated = false;
        int count = 0;

        // Get *all* item value for the current DataItem and ItemValueDefinition.
        List<BaseItemValue> itemValues =
                new ArrayList<BaseItemValue>(
                        getAllItemValues(filter.getDataItem(), filter.getItemValueDefinition().getPath()));

        // Sort so earliest item value comes first, based on the startDate.
        Collections.sort(itemValues, new BaseItemValueStartDateComparator());

        // Create and populate a ResultsWrapper.
        List<BaseDataItemValue> results = new ArrayList<BaseDataItemValue>();
        for (BaseItemValue biv : itemValues) {
            BaseDataItemValue bdiv = (BaseDataItemValue) biv;
            if (BaseItemValueStartDateComparator.isHistoricValue(bdiv)) {
                ExternalHistoryValue ehv = (ExternalHistoryValue) bdiv;
                // At or beyond the start date?
                if (ehv.getStartDate().compareTo(filter.getStartDate()) >= 0) {
                    // Before the end date?
                    if (ehv.getStartDate().before(filter.getEndDate())) {
                        // On or after the resultStart?
                        if (count >= filter.getResultStart()) {
                            // Before the resultLimit?
                            if (results.size() < filter.getResultLimit()) {
                                // Safe to add this item value.
                                results.add(bdiv);
                            } else {
                                // Gone beyond the resultLimit.
                                // The results are truncated and we can ignore the other item values.
                                truncated = true;
                                break;
                            }
                        }
                        // Increment count of eligible item values.
                        count++;
                    } else {
                        // Gone beyond the end date and we can ignore the other item values.
                        break;
                    }
                } else {
                    // Before the start date.
                }
            } else {
                // We should only execute this section once.
                if (handledFirst) {
                    // Should never get here. Implies that the list contains a non-historical item value
                    // is in the wrong place in the list.
                    throw new IllegalStateException("Unexpected non-historical item value: " + biv);
                }
                // On or after the resultStart? Filter at the min date?
                if ((count >= filter.getResultStart()) && filter.getStartDate().equals(DataItemService.MYSQL_MIN_DATETIME)) {
                    // This *must* be the first item value.
                    results.add(bdiv);
                }
                // Increment count of eligible item values.
                count++;
                // We only work in this section once.
                handledFirst = true;
            }
        }

        // Create the ResultsWrapper and return.
        return new ResultsWrapper<BaseDataItemValue>(results, truncated);
    }



    /**
     * Get an {@link BaseItemValue} belonging to this Item using some identifier and prevailing datetime context.
     *
     * @param identifier - a value to be compared to the path and then the uid of the Item Values belonging
     *                   to this Item.
     * @return the matched {@link BaseItemValue} or NULL if no match is found.
     */
    @Override
    public BaseItemValue getItemValue(BaseItem item, String identifier) {
        if (!DataItem.class.isAssignableFrom(item.getClass())) {
            throw new IllegalStateException("A DataItem instance was expected.");
        }
        return getItemValue(item, identifier, item.getEffectiveStartDate());
    }

    /**
     * Updates the Data Item Values for the supplied DataItem based on the properties of the values
     * bean within the DataItem. Internally uses the Spring and Java beans API to access values in the
     * CGLIB created DataItem.values JavaBean.
     * <p/>
     * If a Data Item Value is modified then the Data Item is also marked as modified.
     *
     * @param dataItem to update
     */
    @Override
    public void updateDataItemValues(DataItem dataItem) {
        boolean modified = false;
        Object values = dataItem.getValues();
        Object units = dataItem.getUnits();
        Object perUnits = dataItem.getPerUnits();
        ItemValueMap itemValues = getItemValuesMap(dataItem);
        for (String key : itemValues.keySet()) {
            BaseItemValue value = itemValues.get(key);

            // Values
            PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(values.getClass(), key);
            if (pd != null) {
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    try {
                        Object v = readMethod.invoke(values);
                        if (v != null) {
                            value.setValue(v.toString());
                            modified = true;
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Caught IllegalAccessException: " + e.getMessage());
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Caught InvocationTargetException: " + e.getMessage());
                    }
                } else {
                    log.warn("updateDataItemValues() Read Method was null: " + key);
                }
            } else {
                log.warn("updateDataItemValues() PropertyDescriptor was null: " + key);
            }

            // Units (only number values have units and perUnits)
            if (ExternalNumberValue.class.isAssignableFrom(value.getClass())) {

                // Unit
                pd = BeanUtils.getPropertyDescriptor(units.getClass(), key);
                if (pd != null) {
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        try {
                            Object v = readMethod.invoke(units);
                            if (v != null) {
                                ((DataItemNumberValue) value).setUnit(v.toString());
                                modified = true;
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Caught IllegalAccessException: " + e.getMessage(), e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException("Caught InvocationTargetException: " + e.getMessage(), e);
                        }
                    } else {
                        log.warn("updateDataItemValues() Read Method was null: " + key);
                    }
                } else {
                    log.warn("updateDataItemValues() PropertyDescriptor was null: " + key);
                }

                // Per Unit
                pd = BeanUtils.getPropertyDescriptor(perUnits.getClass(), key);
                if (pd != null) {
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        try {
                            Object v = readMethod.invoke(perUnits);
                            if (v != null) {
                                ((DataItemNumberValue) value).setPerUnit(v.toString());
                                modified = true;
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Caught IllegalAccessException: " + e.getMessage(), e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException("Caught InvocationTargetException: " + e.getMessage(), e);
                        }
                    } else {
                        log.warn("updateDataItemValues() Read Method was null: " + key);
                    }
                } else {
                    log.warn("updateDataItemValues() PropertyDescriptor was null: " + key);
                }
            }
        }

        // Mark the DataItem as modified.
        if (modified) {
            dataItem.onModify();
        }
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

    /**
     * Get an {@code ItemValueMap} containing the given DataItem's DrillDown values.
     *
     * @param dataItem the DataItem to get the drilldown values for.
     * @return an ItemValueMap with the drilldown values.
     */
    @Override
    public ItemValueMap getDrillDownValuesMap(DataItem dataItem) {

        // First get all the item values
        ItemValueMap allValuesMap = getItemValuesMap(dataItem);

        // Then make a new map with just the drillDowns values.
        ItemValueMap drillDownValuesMap = new ItemValueMap();
        List<Choice> drillDownChoices = dataItem.getItemDefinition().getDrillDownChoices();
        for (Choice choice : drillDownChoices) {
            drillDownValuesMap.put(allValuesMap.get(choice.getValue()).getDisplayPath(), allValuesMap.get(choice.getValue()));
        }
        return drillDownValuesMap;
    }

    private List<String> getDrillDownPaths(DataItem dataItem) {

        // First get all the item values
        ItemValueMap allValuesMap = getItemValuesMap(dataItem);

        // Then make a list of the drill down paths
        List<String> drillDownPaths = new ArrayList<String>();
        List<Choice> drillDownChoices = dataItem.getItemDefinition().getDrillDownChoices();
        for (Choice choice : drillDownChoices) {
            drillDownPaths.add(allValuesMap.get(choice.getValue()).getDisplayPath());
        }
        return drillDownPaths;
    }
}
