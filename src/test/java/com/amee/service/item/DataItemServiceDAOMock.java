package com.amee.service.item;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.item.data.DataItemNumberValue;
import com.amee.domain.item.data.DataItemTextValue;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public class DataItemServiceDAOMock implements DataItemServiceDAO {

    @Override
    public List<DataItem> getDataItems(IDataCategoryReference dataCategory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataItem> getDataItems(Set<Long> dataItemIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataItem getItemByUid(String uid) {
        return null;
    }

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<BaseItemValue> getItemValuesForItems(Collection<BaseItem> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(BaseItemValue itemValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<BaseItemValue> getDataItemValues(DataItem dataItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataItemNumberValue> getDataItemNumberValues(DataItem dataItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataItemTextValue> getDataItemTextValues(DataItem dataItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(DataItem dataItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataItem getDataItemByPath(IDataCategoryReference parent, String path) {
        throw new UnsupportedOperationException();
    }
}