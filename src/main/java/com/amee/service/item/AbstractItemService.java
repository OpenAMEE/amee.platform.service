package com.amee.service.item;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.*;
import com.amee.domain.DataItemService;
import com.amee.domain.ItemService;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.data.ItemValueMap;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.BaseDataItemValue;
import com.amee.domain.item.profile.BaseProfileItemValue;
import com.amee.persist.BaseEntity;
import com.amee.platform.science.ExternalHistoryValue;
import com.amee.platform.science.StartEndDate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.*;

public abstract class AbstractItemService implements ItemService, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    protected LocaleService localeService;

    // A thread bound Map of BaseItemValues keyed by BaseItem entity identity.
    private final ThreadLocal<Map<String, Set<BaseItemValue>>> ITEM_VALUES =
            new ThreadLocal<Map<String, Set<BaseItemValue>>>() {
                protected Map<String, Set<BaseItemValue>> initialValue() {
                    return new HashMap<String, Set<BaseItemValue>>();
                }
            };

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.trace("onApplicationEvent() BEFORE_BEGIN");
                    // Reset thread bound data.
                    clearItemValues();
                    break;
                case END:
                    log.trace("onApplicationEvent() END");
                    // Reset thread bound data.
                    clearItemValues();
                    break;
                default:
                    // Do nothing!
            }
        }
    }

    public abstract BaseItem getItemByUid(String uid);

    /**
     * Get a Set of ItemValueDefinitions currently in use by the supplied BaseItem.
     *
     * @param item BaseItem to fetch ItemValueDefinitions for
     * @return Set of ItemValueDefinitions currently in use
     */
    @Override
    public Set<ItemValueDefinition> getItemValueDefinitionsInUse(BaseItem item) {
        Set<ItemValueDefinition> itemValueDefinitions = new HashSet<ItemValueDefinition>();
        for (BaseItemValue itemValue : getActiveItemValues(item)) {
            itemValueDefinitions.add(itemValue.getItemValueDefinition());
        }
        return itemValueDefinitions;
    }

    /**
     * Get a List of all BaseItemValues associated with the supplied BaseItem.
     *
     * @param item BaseItem to fetch BaseItemValues for
     * @return List of BaseItemValues
     */
    @Override
    public List<BaseItemValue> getItemValues(BaseItem item) {
        return getItemValuesMap(item).getAll(item.getEffectiveStartDate());
    }

    /**
     * Get an unmodifiable List of ALL {@link com.amee.domain.item.BaseItemValue}s owned by this Item for a particular {@link com.amee.domain.data.ItemValueDefinition}
     *
     * @param itemValuePath - the {@link com.amee.domain.data.ItemValueDefinition} path
     * @return - the List of {@link com.amee.domain.item.BaseItemValue}
     */
    @Override
    public List<BaseItemValue> getAllItemValues(BaseItem item, String itemValuePath) {
        return Collections.unmodifiableList(getItemValuesMap(item).getAll(itemValuePath));
    }

    @Override
    public Set<BaseItemValue> getActiveItemValues(BaseItem item) {
        Set<BaseItemValue> activeItemValues = new HashSet<BaseItemValue>();
        for (BaseItemValue iv : getAllItemValues(item)) {
            if (!iv.isTrash()) {
                activeItemValues.add(iv);
            }
        }
        return activeItemValues;
    }

    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        Set<BaseItemValue> itemValues = ITEM_VALUES.get().get(item.toString());
        if (itemValues == null) {
            itemValues = getDao().getAllItemValues(item);
            ITEM_VALUES.get().put(item.toString(), itemValues);
        }
        return itemValues;
    }

    /**
     * Attempt to match an {@link com.amee.domain.item.BaseItemValue} belonging to this Item using some identifier. The identifier may be a path
     * or UID.
     *
     * @param identifier - a value to be compared to the path and then the uid of the {@link com.amee.domain.item.BaseItemValue}s belonging
     *                   to this Item.
     * @param startDate  - the startDate to use in the {@link com.amee.domain.item.BaseItemValue} lookup
     * @return the matched {@link com.amee.domain.item.BaseItemValue} or NULL if no match is found.
     */
    @Override
    public BaseItemValue getItemValue(BaseItem item, String identifier, Date startDate) {
        BaseItemValue iv = getItemValuesMap(item).get(identifier, startDate);
        if (iv == null) {
            iv = getByUid(item, identifier);
        }
        return iv;
    }

    /**
     * Get an {@link com.amee.domain.item.BaseItemValue} by UID
     *
     * @param item
     * @param uid  - the {@link com.amee.domain.item.BaseItemValue} UID
     * @return the {@link com.amee.domain.item.BaseItemValue} if found or NULL
     */
    public BaseItemValue getByUid(BaseItem item, final String uid) {
        return (BaseItemValue) CollectionUtils.find(getActiveItemValues(item), new Predicate() {
            public boolean evaluate(Object o) {
                BaseEntity iv = (BaseEntity) o;
                return iv.getUid().equals(uid);
            }
        });
    }

    /**
     * Return an {@link com.amee.domain.data.ItemValueMap} of {@link com.amee.domain.item.BaseItemValue}s belonging
     * to the supplied item.
     * The key is the value returned by {@link BaseItemValue#getDisplayPath()}.
     *
     * @param item
     * @return {@link com.amee.domain.data.ItemValueMap}
     */
    @Override
    public ItemValueMap getItemValuesMap(BaseItem item) {
        ItemValueMap itemValuesMap = new ItemValueMap();
        for (BaseItemValue itemValue : getActiveItemValues(item)) {
            itemValuesMap.put(itemValue.getDisplayPath(), itemValue);
        }
        return itemValuesMap;
    }

    /**
     * Check if there exists amongst the current set of BaseItemValues, an entry with the given
     * itemValueDefinition and startDate.
     *
     * @param itemValueDefinition - an {@link com.amee.domain.data.ItemValueDefinition}
     * @param startDate           - an {@link com.amee.platform.science.StartEndDate} startDate
     * @return - true if the newItemValue is unique, otherwise false
     */
    @Override
    public boolean isItemValueUnique(BaseItem item, ItemValueDefinition itemValueDefinition, StartEndDate startDate) {
        String uniqueId = itemValueDefinition.getUid() + startDate.getTime();
        for (BaseItemValue iv : getActiveItemValues(item)) {
            long time = ExternalHistoryValue.class.isAssignableFrom(iv.getClass()) ?
                    ((ExternalHistoryValue) iv).getStartDate().getTime() :
                    DataItemService.EPOCH.getTime();
            String checkId = iv.getItemValueDefinition().getUid() + time;
            if (uniqueId.equals(checkId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a List of all BaseItemValues associated with the supplied BaseItem.
     *
     * @param items BaseItems to fetch BaseItemValues for
     */
    @Override
    public void loadItemValuesForItems(Collection<BaseItem> items) {
        // A null entry for when there are no BaseItemValues for the entity.
        // Ensure a null entry exists for all BaseItemValues.
        for (IAMEEEntityReference item : items) {
            if (!ITEM_VALUES.get().containsKey(item.toString())) {
                ITEM_VALUES.get().put(item.toString(), null);
            }
        }
        // Store BaseItemValues against BaseItems.
        // If there are no BaseItemValues for a BaseItem the entry will remain null.
        for (BaseItemValue itemValue : getDao().getItemValuesForItems(items)) {
            Set<BaseItemValue> itemValues = ITEM_VALUES.get().get(itemValue.getItem().toString());
            if (itemValues == null) {
                itemValues = new HashSet<BaseItemValue>();
            }
            itemValues.add(itemValue);
            ITEM_VALUES.get().put(itemValue.getItem().toString(), itemValues);
        }
    }

    @Override
    public void addItemValue(BaseItemValue itemValue) {
        getDao().persist(itemValue);
        clearItemValues();
    }

    @Override
    public void clearItemValues() {
        ITEM_VALUES.get().clear();
    }

    @Override
    public StartEndDate getStartDate(BaseItemValue itemValue) {
        if (BaseProfileItemValue.class.isAssignableFrom(itemValue.getClass())) {
            return ((BaseProfileItemValue) itemValue).getProfileItem().getStartDate();
        } else if (BaseDataItemValue.class.isAssignableFrom(itemValue.getClass())) {
            if (ExternalHistoryValue.class.isAssignableFrom(itemValue.getClass())) {
                return ((ExternalHistoryValue) itemValue).getStartDate();
            } else {
                return new StartEndDate(DataItemService.EPOCH);
            }
        } else {
            throw new IllegalStateException("A BaseProfileItemValue or BaseDataItemValue instance was expected.");
        }
    }

    protected abstract ItemServiceDAO getDao();
}
