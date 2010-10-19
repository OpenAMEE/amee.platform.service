package com.amee.service.item;

import com.amee.domain.IItemService;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.platform.science.StartEndDate;

import java.util.Date;
import java.util.List;
import java.util.Set;

public abstract class ItemService implements IItemService {

    public abstract BaseItem getItemByUid(String uid);

    /**
     * Get a Set of ItemValueDefinitions currently in use by the supplied BaseItem.
     *
     * @param item BaseItem to fetch ItemValueDefinitions for
     * @return Set of ItemValueDefinitions currently in use
     */
    public Set<ItemValueDefinition> getItemValueDefinitionsInUse(BaseItem item) {
        // TODO: See com.amee.domain.data.LegacyItem.getItemValueDefinitions.
        throw new UnsupportedOperationException();
    }

    /**
     * Get a List of all BaseItemValues associated with the supplied BaseItem.
     *
     * @param item BaseItem to fetch BaseItemValues for
     * @return List of BaseItemValues
     */
    public List<BaseItemValue> getItemValues(BaseItem item) {
        // TODO: See com.amee.domain.data.LegacyItem.getItemValues.
        throw new UnsupportedOperationException();
    }

    public List<BaseItemValue> getAllItemValues(BaseItem item, String itemValuePath) {
        // TODO: See com.amee.domain.data.LegacyItem#getAllItemValues.
        throw new UnsupportedOperationException();
    }

    public BaseItemValue getItemValue(BaseItem item, String identifier, Date startDate) {
        // TODO: See com.amee.domain.data.LegacyItem#getItemValue.
        throw new UnsupportedOperationException();
    }

    public BaseItemValue getItemValue(BaseItem item, String identifier) {
        // TODO: See com.amee.domain.data.LegacyItem#getItemValue.
        throw new UnsupportedOperationException();
    }

    public boolean isUnique(BaseItem item, ItemValueDefinition itemValueDefinition, StartEndDate startDate) {
        // TODO: com.amee.domain.data.LegacyItem#isUnique.
        throw new UnsupportedOperationException();
    }
}
