package com.amee.service.item;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IDataItemService;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValue;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.NuDataItem;
import com.amee.domain.sheet.Choice;
import com.amee.platform.science.StartEndDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class DataItemService extends ItemService implements IDataItemService {

    @Autowired
    private DataItemServiceDAO dao;

    public List<NuDataItem> getDataItems(DataCategory dataCategory) {
        return dao.getDataItems(dataCategory);
    }

    public List<NuDataItem> getDataItems(Set<Long> dataItemIds) {
        return dao.getDataItems(dataItemIds);
    }

    public NuDataItem getItemByUid(String uid) {
        NuDataItem dataItem = dao.getItemByUid(uid);
        if ((dataItem != null) && (!dataItem.isTrash())) {
            return dataItem;
        } else {
            return null;
        }
    }

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

    private ItemValue getItemValue(String choiceName) {
        throw new UnsupportedOperationException();
    }


    // TODO: Implement 'effective' parameter support.

    public Date getEffectiveStartDate(BaseItem item) {
        return new StartEndDate(IDataItemService.EPOCH);
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
