package com.amee.service.item;

import com.amee.base.utils.XMLUtils;
import com.amee.domain.AMEEStatus;
import com.amee.domain.IDataItemService;
import com.amee.domain.TimeZoneHolder;
import com.amee.domain.data.*;
import com.amee.domain.data.builder.v2.ItemValueBuilder;
import com.amee.domain.environment.Environment;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.NuDataItem;
import com.amee.domain.sheet.Choice;
import com.amee.platform.science.StartEndDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    private ItemValue getItemValue(String choiceName) {
        throw new UnsupportedOperationException();
    }

    public JSONObject getJSONObject(NuDataItem dataItem, boolean detailed, boolean showHistory) throws JSONException {
        JSONObject obj = new JSONObject();
        buildJSON(dataItem, obj, detailed, showHistory);
        obj.put("path", dataItem.getPath());
        obj.put("label", getLabel(dataItem));
        obj.put("startDate", StartEndDate.getLocalStartEndDate(new StartEndDate(EPOCH), TimeZoneHolder.getTimeZone()).toString());
        obj.put("endDate", "");
        return obj;
    }

    public JSONObject getJSONObject(NuDataItem dataItem, boolean detailed) throws JSONException {
        return getJSONObject(dataItem, detailed, false);
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
        for (BaseItemValue baseItemValue : getItemValues(dataItem)) {
            ItemValue itemValue = new ItemValue(baseItemValue);
            itemValue.setBuilder(new ItemValueBuilder(itemValue));
            itemValues.put(itemValue.getJSONObject(false));
        }
    }

    private void buildJSONItemValuesWithHistory(NuDataItem dataItem, JSONArray itemValues) throws JSONException {
        for (Object o1 : getItemValuesMap(dataItem).keySet()) {
            String path = (String) o1;
            JSONObject values = new JSONObject();
            JSONArray valueSet = new JSONArray();
            for (Object o2 : getAllItemValues(dataItem, path)) {
                LegacyItemValue itemValue = (LegacyItemValue) o2;
                itemValue.setBuilder(new ItemValueBuilder(itemValue));
                valueSet.put(itemValue.getJSONObject(false));
            }
            values.put(path, valueSet);
            itemValues.put(values);
        }
    }

    public Element getElement(NuDataItem dataItem, Document document, boolean detailed, boolean showHistory) {
        Element dataItemElement = document.createElement("DataItem");
        buildElement(dataItem, document, dataItemElement, detailed, showHistory);
        dataItemElement.appendChild(XMLUtils.getElement(document, "Path", dataItem.getDisplayPath()));
        dataItemElement.appendChild(XMLUtils.getElement(document, "Label", getLabel(dataItem)));
        dataItemElement.appendChild(XMLUtils.getElement(document, "StartDate",
                StartEndDate.getLocalStartEndDate(
                        dataItem.getAdapter().getStartDate(), TimeZoneHolder.getTimeZone()).toString()));

        Date endDate = dataItem.getAdapter().getEndDate();
        dataItemElement.appendChild(XMLUtils.getElement(document, "EndDate",
                (endDate != null) ? StartEndDate.getLocalStartEndDate(endDate, TimeZoneHolder.getTimeZone()).toString() : ""));
        return dataItemElement;
    }

    public Element getElement(NuDataItem dataItem, Document document, boolean detailed) {
        return getElement(dataItem, document, detailed, false);
    }

    private void buildElement(NuDataItem dataItem, Document document, Element element, boolean detailed, boolean showHistory) {
        element.setAttribute("uid", dataItem.getUid());
        element.appendChild(XMLUtils.getElement(document, "Name", dataItem.getDisplayName()));
        Element itemValuesElem = document.createElement("ItemValues");
        if (showHistory) {
            buildElementItemValuesWithHistory(dataItem, document, itemValuesElem);
        } else {
            buildElementItemValues(dataItem, document, itemValuesElem);
        }
        element.appendChild(itemValuesElem);
        if (detailed) {
            element.setAttribute("created", dataItem.getCreated().toString());
            element.setAttribute("modified", dataItem.getModified().toString());
            element.appendChild(Environment.ENVIRONMENT.getIdentityElement(document));
            element.appendChild(dataItem.getItemDefinition().getIdentityElement(document));
            element.appendChild(dataItem.getDataCategory().getIdentityElement(document));
        }
    }

    private void buildElementItemValues(NuDataItem dataItem, Document document, Element itemValuesElem) {
        for (BaseItemValue baseItemValue : getItemValues(dataItem)) {
            ItemValue itemValue = new ItemValue(baseItemValue);
            itemValue.setBuilder(new ItemValueBuilder(itemValue));
            itemValuesElem.appendChild(itemValue.getElement(document, false));
        }
    }

    private void buildElementItemValuesWithHistory(NuDataItem dataItem, Document document, Element itemValuesElem) {
        for (Object o1 : getItemValuesMap(dataItem).keySet()) {
            String path = (String) o1;
            Element itemValueSeries = document.createElement("ItemValueSeries");
            itemValueSeries.setAttribute("path", path);
            for (Object o2 : getAllItemValues(dataItem, path)) {
                LegacyItemValue itemValue = (LegacyItemValue) o2;
                itemValue.setBuilder(new ItemValueBuilder(itemValue));
                itemValueSeries.appendChild(itemValue.getElement(document, false));
            }
            itemValuesElem.appendChild(itemValueSeries);
        }
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
