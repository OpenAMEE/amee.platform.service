/**
 * This file is part of AMEE.
 *
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
package com.amee.calculation.service;

import com.amee.domain.AMEEStatistics;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemValue;
import com.amee.domain.profile.ProfileItem;
import com.amee.service.profile.ProfileService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class ProfileFinder implements Serializable {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AMEEStatistics ameeStatistics;

    private DataFinder dataFinder;
    private ProfileItem profileItem;

    public ProfileFinder() {
        super();
    }

    public String getProfileItemValue(String path, String name) {
        String value = null;
        ItemValue iv;
        ProfileItem pi = getProfileItem(path);
        if (pi != null) {
            iv = pi.getItemValue(name);
            if (iv != null) {
                value = iv.getValue();
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("getProfileItemValue() - path: " + path + ", name: " + name + ", value: " + value);
        }
        return value;
    }

    public void setProfileItemValue(String path, String name, String value) {
        ItemValue iv;
        ProfileItem pi = getProfileItem(path);
        if (pi != null) {
            iv = pi.getItemValue(name);
            if (iv != null) {
                iv.setValue(value);
                ameeStatistics.updateProfileItemValue();
            }
        }
    }

    public void setProfileItemValue(String name, String value) {
        ItemValue iv;
        if (profileItem != null) {
            iv = profileItem.getItemValue(name);
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
            profileItems = profileService.getProfileItems(
                    profileItem.getProfile(),
                    profileItem.getDataCategory(),
                    profileItem.getStartDate());
        }
        return profileItems;
    }

    public List<ProfileItem> getProfileItems(String path) {
        List<ProfileItem> profileItems = new ArrayList<ProfileItem>();
        if (profileItem != null) {
            DataCategory dataCategory = dataFinder.getDataCategory(path);
            if (dataCategory != null) {
                profileItems = profileService.getProfileItems(
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
