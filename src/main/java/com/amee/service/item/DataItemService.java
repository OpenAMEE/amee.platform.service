package com.amee.service.item;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IDataItemService;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.NuDataItem;
import com.amee.domain.sheet.Choice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class DataItemService extends ItemService implements IDataItemService {

    @Autowired
    private DataItemServiceDAO dao;

    @Override
    public List<NuDataItem> getDataItems(DataCategory dataCategory) {
        List<NuDataItem> dataItems = dao.getDataItems(dataCategory);
        loadItemValuesForItems((List) dataItems);
        return dataItems;
    }

    @Override
    public List<NuDataItem> getDataItems(Set<Long> dataItemIds) {
        List<NuDataItem> dataItems = dao.getDataItems(dataItemIds);
        loadItemValuesForItems((List) dataItems);
        return dataItems;
    }

    @Override
    public NuDataItem getItemByUid(String uid) {
        NuDataItem dataItem = dao.getItemByUid(uid);
        if ((dataItem != null) && (!dataItem.isTrash())) {
            return dataItem;
        } else {
            return null;
        }
    }

    @Override
    public DataItem getDataItemByPath(DataCategory parent, String path) {
        NuDataItem dataItem = dao.getDataItemByPath(parent, path);
        return DataItem.getDataItem(dataItem);
    }

    @Override
    public String getLabel(NuDataItem dataItem) {
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
    public void remove(DataItem dataItem) {
        dataItem.getNuEntity().setStatus(AMEEStatus.TRASH);
    }

    public void remove(BaseItemValue itemValue) {
        itemValue.setStatus(AMEEStatus.TRASH);
    }

    /**
     * Get an {@link com.amee.domain.data.LegacyItemValue} belonging to this Item using some identifier and prevailing datetime context.
     *
     * @param identifier - a value to be compared to the path and then the uid of the {@link com.amee.domain.data.LegacyItemValue}s belonging
     *                   to this Item.
     * @return the matched {@link com.amee.domain.data.LegacyItemValue} or NULL if no match is found.
     */
    @Override
    public BaseItemValue getItemValue(BaseItem item, String identifier) {
        if (!NuDataItem.class.isAssignableFrom(item.getClass()))
            throw new IllegalStateException("A NuDataItem instance was expected.");
        return getItemValue(item, identifier, item.getEffectiveStartDate());
    }

    public void persist(NuDataItem dataItem) {
        dao.persist(dataItem);
    }

    // ItemValues.

    public void persist(BaseItemValue itemValue) {
        dao.persist(itemValue);
    }

    @Override
    protected DataItemServiceDAO getDao() {
        return dao;
    }
}
