package com.amee.service.item;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.ProfileItem;
import com.amee.domain.item.profile.ProfileItemNumberValue;
import com.amee.domain.item.profile.ProfileItemTextValue;
import com.amee.domain.profile.Profile;
import com.amee.platform.science.StartEndDate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Repository
public class ProfileItemServiceDAOMock implements ProfileItemServiceDAO {

    @Override
    public ProfileItem getItemByUid(String uid) {
        return null;
    }

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<BaseItemValue> getItemValuesForItems(Collection<BaseItem> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<BaseItemValue> getProfileItemValues(ProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProfileItemNumberValue> getProfileItemNumberValues(ProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProfileItemTextValue> getProfileItemTextValues(ProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getProfileItemCount(Profile profile, DataCategory dataCategory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, Date profileDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, StartEndDate startDate, StartEndDate endDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equivalentProfileItemExists(ProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Long> getProfileDataCategoryIds(Profile profile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(BaseItemValue itemValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(ProfileItem profileItem) {
        throw new UnsupportedOperationException();
    }
}