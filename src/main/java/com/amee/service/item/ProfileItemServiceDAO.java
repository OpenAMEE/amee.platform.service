package com.amee.service.item;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.ProfileItem;
import com.amee.domain.item.profile.ProfileItemNumberValue;
import com.amee.domain.item.profile.ProfileItemTextValue;
import com.amee.domain.profile.Profile;
import com.amee.platform.science.StartEndDate;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface ProfileItemServiceDAO extends ItemServiceDAO {

    @Override
    public ProfileItem getItemByUid(String uid);

    public Set<BaseItemValue> getProfileItemValues(ProfileItem profileItem);

    public List<ProfileItemNumberValue> getProfileItemNumberValues(ProfileItem profileItem);

    public List<ProfileItemTextValue> getProfileItemTextValues(ProfileItem profileItem);

    public int getProfileItemCount(Profile profile, DataCategory dataCategory);

    public List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, Date profileDate);

    public List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, StartEndDate startDate, StartEndDate endDate);

    public boolean equivalentProfileItemExists(ProfileItem profileItem);

    public Collection<Long> getProfileDataCategoryIds(Profile profile);

    public void persist(ProfileItem profileItem);
}
