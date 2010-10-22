package com.amee.service.item;

import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.NuProfileItem;
import com.amee.domain.item.profile.ProfileItemNumberValue;
import com.amee.domain.item.profile.ProfileItemTextValue;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public Set<BaseItemValue> getProfileItemValues(NuProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProfileItemNumberValue> getProfileItemNumberValues(NuProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProfileItemTextValue> getProfileItemTextValues(NuProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(BaseItemValue itemValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(NuProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }
}