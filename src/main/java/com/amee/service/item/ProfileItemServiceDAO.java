package com.amee.service.item;

import com.amee.domain.item.profile.NuProfileItem;

public interface ProfileItemServiceDAO extends ItemServiceDAO {

    public NuProfileItem getItemByUid(String uid);
}
