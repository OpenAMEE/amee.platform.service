package com.amee.service.item;

import com.amee.domain.IDataItemService;
import com.amee.domain.TimeZoneHolder;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValue;
import com.amee.domain.data.LegacyItemValue;
import com.amee.domain.data.builder.v2.ItemValueBuilder;
import com.amee.domain.environment.Environment;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.BaseDataItemTextValue;
import com.amee.domain.item.data.NuDataItem;
import com.amee.domain.sheet.Choice;
import com.amee.platform.science.StartEndDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public JSONObject getJSONObject(NuDataItem dataItem, boolean detailed, boolean showHistory) throws JSONException {
        JSONObject obj = new JSONObject();
        buildJSON(dataItem, obj, detailed, showHistory);
        obj.put("path", dataItem.getPath());
        obj.put("label", getLabel(dataItem));
        obj.put("startDate", StartEndDate.getLocalStartEndDate(dataItem.getStartDate(), TimeZoneHolder.getTimeZone()).toString());

        // TODO: Confirm correct - the ternary condition looks dubious given that DataItem#getEndDate always returns null.
        obj.put("endDate",
                (dataItem.getEndDate() != null) ? StartEndDate.getLocalStartEndDate(dataItem.getEndDate(), TimeZoneHolder.getTimeZone()).toString() : "");
        return obj;

    }

    private void buildJSON(NuDataItem dataItem, JSONObject obj, boolean detailed, boolean showHistory) throws JSONException {
        obj.put("uid", dataItem.getUid());
        obj.put("name", dataItem.getDisplayName());
        JSONArray itemValues = new JSONArray();
        if (showHistory) {
            buildJSONItemValuesWithHistory(dataItem, itemValues);
        } else {
            buildJSONItemValues(dataItem, itemValues);
        }
        obj.put("itemValues", itemValues);
        if (detailed) {
            obj.put("created", dataItem.getCreated());
            obj.put("modified", dataItem.getModified());
            obj.put("environment", Environment.ENVIRONMENT.getJSONObject());
            obj.put("itemDefinition", dataItem.getItemDefinition().getJSONObject());
            obj.put("dataCategory", dataItem.getDataCategory().getIdentityJSONObject());
        }
    }

    private void buildJSONItemValues(NuDataItem dataItem, JSONArray itemValues) throws JSONException {
        for (BaseItemValue itemValue : itemService.getItemValues(dataItem))  {
            itemValue.setBuilder(new ItemValueBuilder(itemValue));
            itemValues.put(itemValue.getJSONObject(false));
        }
    }

    private void buildJSONItemValuesWithHistory(NuDataItem dataItem, JSONArray itemValues) throws JSONException {
        for (Object o1 : getItemValuesMap().keySet()) {
            String path = (String) o1;
            JSONObject values = new JSONObject();
            JSONArray valueSet = new JSONArray();
            for (Object o2 : getAllItemValues(path)) {
                LegacyItemValue itemValue = (LegacyItemValue) o2;
                itemValue.setBuilder(new ItemValueBuilder(itemValue));
                valueSet.put(itemValue.getJSONObject(false));
            }
            values.put(path, valueSet);
            itemValues.put(values);
        }
    }

    

}
