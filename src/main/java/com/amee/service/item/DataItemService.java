package com.amee.service.item;

import com.amee.domain.IDataItemService;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValue;
import com.amee.domain.item.data.BaseDataItemTextValue;
import com.amee.domain.item.data.NuDataItem;
import com.amee.domain.sheet.Choice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataItemService extends ItemService implements IDataItemService {

    @Autowired
    private DataItemServiceDAO dao;

    @Autowired
    private ItemService itemService;

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
        BaseDataItemTextValue itemValue = null;

        ItemDefinition itemDefinition = dataItem.getItemDefinition();
        for (Choice choice : itemDefinition.getDrillDownChoices()) {
            itemValue = (BaseDataItemTextValue) itemService.getItemValue(dataItem, choice.getName());
            if ((itemValue != null) &&
                    (itemValue.getValue().length() > 0) &&
                    !itemValue.getValue().equals("-")) {
                if (label.length() > 0) {
                    label = label.concat(", ");
                }
                label = label.concat(itemValue.getValue());
            }
        }
        if (label.length() == 0) {
            label = dataItem.getDisplayPath();
        }
        return label;
    }

    private ItemValue getItemValue(String choiceName) {
        throw new UnsupportedOperationException();
    }
}
