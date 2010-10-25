package com.amee.service.item;

import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;

import java.util.Collection;
import java.util.Set;

public interface ItemServiceDAO {

    public BaseItem getItemByUid(String uid);

    public Set<BaseItemValue> getAllItemValues(BaseItem item);

    public Set<BaseItemValue> getItemValuesForItems(Collection<BaseItem> items);

    public void persist(BaseItemValue itemValue);
}
