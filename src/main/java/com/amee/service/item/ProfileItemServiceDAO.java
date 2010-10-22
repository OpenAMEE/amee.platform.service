package com.amee.service.item;

import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.NuProfileItem;
import com.amee.domain.item.profile.ProfileItemNumberValue;
import com.amee.domain.item.profile.ProfileItemTextValue;

import java.util.List;
import java.util.Set;

public interface ProfileItemServiceDAO extends ItemServiceDAO {

    @Override
    public NuProfileItem getItemByUid(String uid);

    public Set<BaseItemValue> getProfileItemValues(NuProfileItem profileItem);

    public List<ProfileItemNumberValue> getProfileItemNumberValues(NuProfileItem profileItem);

    public List<ProfileItemTextValue> getProfileItemTextValues(NuProfileItem profileItem);

    public void persist(NuProfileItem profileItem);
}