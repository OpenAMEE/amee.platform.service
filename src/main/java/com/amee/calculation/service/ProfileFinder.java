package com.amee.calculation.service;

import com.amee.domain.AMEEStatistics;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.ProfileItemService;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.ProfileItem;
import com.amee.service.profile.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class ProfileFinder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProfileItemService profileItemService;

    @Autowired
    private AMEEStatistics ameeStatistics;

    private DataFinder dataFinder;
    private ProfileItem profileItem;

    public ProfileFinder() {
        super();
    }

    public String getProfileItemValue(String path, String name) {
        String value = null;
        BaseItemValue iv;
        ProfileItem pi = getProfileItem(path);
        if (pi != null) {
            iv = profileItemService.getItemValue(pi, name);
            if (iv != null) {
                value = iv.getValueAsString();
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("getProfileItemValue() - path: " + path + ", name: " + name + ", value: " + value);
        }
        return value;
    }

    public void setProfileItemValue(String path, String name, String value) {
        BaseItemValue iv;
        ProfileItem pi = getProfileItem(path);
        if (pi != null) {
            iv = profileItemService.getItemValue(pi, name);
            if (iv != null) {
                iv.setValue(value);
                ameeStatistics.updateProfileItemValue();
            }
        }
    }

    public void setProfileItemValue(String name, String value) {
        BaseItemValue iv;
        if (profileItem != null) {
            iv = profileItemService.getItemValue(profileItem, name);
            if (iv != null) {
                iv.setValue(value);
                ameeStatistics.updateProfileItemValue();
            }
        }
    }

    public ProfileItem getProfileItem(String path) {
        List<ProfileItem> profileItems = getProfileItems(path);
        if (profileItems.size() > 0) {
            return profileItems.get(0);
        } else {
            return null;
        }
    }

    public List<ProfileItem> getProfileItems() {
        List<ProfileItem> profileItems = new ArrayList<ProfileItem>();
        if (profileItem != null) {
            profileItems = profileItemService.getProfileItems(
                    profileItem.getProfile(),
                    profileItem.getDataCategory(),
                    profileItem.getStartDate());
        }
        return profileItems;
    }

    public List<ProfileItem> getProfileItems(String path) {
        List<ProfileItem> profileItems = new ArrayList<ProfileItem>();
        if (profileItem != null) {
            IDataCategoryReference dataCategory = dataFinder.getDataCategory(path);
            if (dataCategory != null) {
                profileItems = profileItemService.getProfileItems(
                        profileItem.getProfile(),
                        dataCategory,
                        profileItem.getStartDate());
            }
        }
        return profileItems;
    }

    public void setDataFinder(DataFinder dataFinder) {
        this.dataFinder = dataFinder;
    }

    public void setProfileItem(ProfileItem profileItem) {
        this.profileItem = profileItem;
    }
}
