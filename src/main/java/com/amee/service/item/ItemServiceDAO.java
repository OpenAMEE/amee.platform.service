package com.amee.service.item;

import com.amee.domain.item.BaseItem;

public interface ItemServiceDAO {

    public BaseItem getItemByUid(String uid);
}
