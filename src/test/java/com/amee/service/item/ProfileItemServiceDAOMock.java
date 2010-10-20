package com.amee.service.item;

import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.NuProfileItem;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class ProfileItemServiceDAOMock implements ProfileItemServiceDAO {

    @Override
    public NuProfileItem getItemByUid(String uid) {
        return null;
    }

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        throw new UnsupportedOperationException();
    }
}