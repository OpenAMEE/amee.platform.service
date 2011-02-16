package com.amee.service.item;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.item.data.DataItemNumberValue;
import com.amee.domain.item.data.DataItemTextValue;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface DataItemServiceDAO extends ItemServiceDAO {

    public long getDataItemCount(IDataCategoryReference dataCategory);

    public List<DataItem> getDataItems(IDataCategoryReference dataCategory);

    public List<DataItem> getDataItems(Set<Long> dataItemIds);

    public DataItem getItemByUid(String uid);

    public Set<BaseItemValue> getDataItemValues(DataItem dataItem);

    public List<DataItemNumberValue> getDataItemNumberValues(DataItem dataItem);

    public List<DataItemTextValue> getDataItemTextValues(DataItem dataItem);

    public void persist(DataItem dataItem);

    public DataItem getDataItemByPath(IDataCategoryReference parent, String path);

    public Date getDataItemsModified(DataCategory dataCategory);
}
