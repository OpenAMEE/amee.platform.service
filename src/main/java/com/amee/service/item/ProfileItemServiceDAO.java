package com.amee.service.item;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.ProfileItemsFilter;
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
    ProfileItem getItemByUid(String uid);

    Set<BaseItemValue> getProfileItemValues(ProfileItem profileItem);

    List<ProfileItemNumberValue> getProfileItemNumberValues(ProfileItem profileItem);

    List<ProfileItemTextValue> getProfileItemTextValues(ProfileItem profileItem);

    int getProfileItemCount(Profile profile, DataCategory dataCategory);

    /**
     * Gets all active profile items for the given profile.
     * An active profile item is one that has an endDate of null or in the future.
     *
     * @param profile the Profile to fetch the profile items for.
     * @param filter a LimitFilter to limit how many profile items are returned.
     * @return a ResultsWrapper containing the profile items.
     */
    ResultsWrapper<ProfileItem> getProfileItems(Profile profile, ProfileItemsFilter filter);

    List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, Date profileDate);

    List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, StartEndDate startDate, StartEndDate endDate);

    boolean equivalentProfileItemExists(ProfileItem profileItem);

    Collection<Long> getProfileDataCategoryIds(Profile profile);

    void persist(ProfileItem profileItem);
}
