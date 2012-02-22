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

    long getDataItemCount(IDataCategoryReference dataCategory);

    List<DataItem> getDataItems(IDataCategoryReference dataCategory);

    List<DataItem> getDataItems(Set<Long> dataItemIds);

    @Override
    DataItem getItemByUid(String uid);

    Set<BaseItemValue> getDataItemValues(DataItem dataItem);

    List<DataItemNumberValue> getDataItemNumberValues(DataItem dataItem);

    List<DataItemTextValue> getDataItemTextValues(DataItem dataItem);

    void persist(DataItem dataItem);

    DataItem getDataItemByPath(IDataCategoryReference parent, String path);

    Date getDataItemsModified(DataCategory dataCategory);

    boolean isDataItemUniqueByPath(DataItem dataItem);
}
