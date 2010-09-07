package com.amee.service.item;

import com.amee.domain.IItemService;
import com.amee.domain.item.BaseItem;

public abstract class ItemService implements IItemService {

    public abstract BaseItem getItemByUid(String uid);
}
