package com.amee.service.item;

import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.DataItemNumberValue;
import com.amee.domain.item.data.DataItemTextValue;
import com.amee.domain.item.data.NuDataItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class DataItemServiceDAOMock implements DataItemServiceDAO {

    @Override
    public NuDataItem getItemByUid(String uid) {
        return null;
    }

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(BaseItemValue itemValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<BaseItemValue> getDataItemValues(NuDataItem dataItem) {
        throw new UnsupportedOperationException();
    }

    public List<DataItemNumberValue> getDataItemNumberValues(NuDataItem dataItem) {
        throw new UnsupportedOperationException();
    }

    public List<DataItemTextValue> getDataItemTextValues(NuDataItem dataItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(NuDataItem dataItem) {
        throw new UnsupportedOperationException();
    }
}