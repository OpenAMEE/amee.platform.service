package com.amee.service.item;

import com.amee.domain.IItemService;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.data.LegacyItemValue;
import com.amee.domain.data.NuItemValueMap;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.platform.science.StartEndDate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import java.util.*;

public abstract class ItemService implements IItemService {

    public abstract BaseItem getItemByUid(String uid);

    /**
     * Get a Set of ItemValueDefinitions currently in use by the supplied BaseItem.
     *
     * @param item BaseItem to fetch ItemValueDefinitions for
     * @return Set of ItemValueDefinitions currently in use
     */
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
    public List<BaseItemValue> getItemValues(BaseItem item) {
        return Collections.unmodifiableList(getItemValuesMap(item).getAll(getEffectiveStartDate(item)));
    }

    public List<BaseItemValue> getAllItemValues(BaseItem item, String itemValuePath) {
        // TODO: See com.amee.domain.data.LegacyItem#getAllItemValues.
        throw new UnsupportedOperationException();
    }

    public Set<BaseItemValue> getActiveItemValues(BaseItem item) {
        // TODO: This should be cached.
        Set<BaseItemValue> activeItemValues = null;
        if (activeItemValues == null) {
            activeItemValues = new HashSet<BaseItemValue>();
            for (BaseItemValue iv : getAllItemValues(item)) {
                if (!iv.isTrash()) {
                    activeItemValues.add(iv);
                }
            }
        }
        return Collections.unmodifiableSet(activeItemValues);
    }

    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        return getDao().getAllItemValues(item);
    }

    /**
     * Attempt to match an {@link com.amee.domain.data.LegacyItemValue} belonging to this Item using some identifier. The identifier may be a path
     * or UID.
     *
     * @param identifier - a value to be compared to the path and then the uid of the {@link com.amee.domain.data.LegacyItemValue}s belonging
     *                   to this Item.
     * @param startDate  - the startDate to use in the {@link com.amee.domain.data.LegacyItemValue} lookup
     * @return the matched {@link com.amee.domain.data.LegacyItemValue} or NULL if no match is found.
     */
    public BaseItemValue getItemValue(BaseItem item, String identifier, Date startDate) {
        BaseItemValue iv = getItemValuesMap(item).get(identifier, startDate);
        if (iv == null) {
            iv = getByUid(item, identifier);
        }
        return iv;
    }

    /**
     * Get an {@link com.amee.domain.data.LegacyItemValue} belonging to this Item using some identifier and prevailing datetime context.
     *
     * @param identifier - a value to be compared to the path and then the uid of the {@link com.amee.domain.data.LegacyItemValue}s belonging
     *                   to this Item.
     * @return the matched {@link com.amee.domain.data.LegacyItemValue} or NULL if no match is found.
     */
    public BaseItemValue getItemValue(BaseItem item, String identifier) {
        return getItemValue(item, identifier, getEffectiveStartDate(item));
    }

    /**
     * Get an {@link com.amee.domain.data.LegacyItemValue} by UID
     *
     * @param item
     * @param uid  - the {@link com.amee.domain.data.LegacyItemValue} UID
     * @return the {@link com.amee.domain.data.LegacyItemValue} if found or NULL
     */
    private BaseItemValue getByUid(BaseItem item, final String uid) {
        return (BaseItemValue) CollectionUtils.find(getActiveItemValues(item), new Predicate() {
            public boolean evaluate(Object o) {
                LegacyItemValue iv = (LegacyItemValue) o;
                return iv.getUid().equals(uid);
            }
        });
    }

    /**
     * Return an {@link com.amee.domain.data.ItemValueMap} of {@link com.amee.domain.data.LegacyItemValue}s belonging to this Item.
     * The key is the value returned by {@link LegacyItemValue#getDisplayPath()}.
     *
     * @param item
     * @return {@link com.amee.domain.data.ItemValueMap}
     */
    public NuItemValueMap getItemValuesMap(BaseItem item) {
        // TODO: This should be cached.
        NuItemValueMap itemValuesMap = null;
        if (itemValuesMap == null) {
            itemValuesMap = new NuItemValueMap();
            for (BaseItemValue itemValue : getActiveItemValues(item)) {
                itemValuesMap.put(itemValue.getDisplayPath(), itemValue);
            }
        }
        return itemValuesMap;
    }

    public boolean isUnique(BaseItem item, ItemValueDefinition itemValueDefinition, StartEndDate startDate) {
        // TODO: com.amee.domain.data.LegacyItem#isUnique.
        throw new UnsupportedOperationException();
    }

    public abstract Date getEffectiveStartDate(BaseItem item);

    protected abstract ItemServiceDAO getDao();
}
