package com.amee.service.item;

import com.amee.domain.item.data.NuDataItem;

public interface DataItemServiceDAO extends ItemServiceDAO {

    public NuDataItem getItemByUid(String uid);
}
