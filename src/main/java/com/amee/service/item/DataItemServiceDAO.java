package com.amee.service.item;

import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItemNumberValue;
import com.amee.domain.item.data.DataItemTextValue;
import com.amee.domain.item.data.NuDataItem;

import java.util.List;
import java.util.Set;

public interface DataItemServiceDAO extends ItemServiceDAO {

    public List<NuDataItem> getDataItems(DataCategory dataCategory);

    public List<NuDataItem> getDataItems(Set<Long> dataItemIds);

    public NuDataItem getItemByUid(String uid);

    public Set<BaseItemValue> getDataItemValues(NuDataItem dataItem);

    public List<DataItemNumberValue> getDataItemNumberValues(NuDataItem dataItem);

    public List<DataItemTextValue> getDataItemTextValues(NuDataItem dataItem);

    public void persist(NuDataItem dataItem);

    public NuDataItem getDataItemByPath(DataCategory parent, String path);
}
