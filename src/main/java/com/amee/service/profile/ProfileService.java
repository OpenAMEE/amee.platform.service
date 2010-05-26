package com.amee.service.profile;

import com.amee.base.transaction.TransactionController;
import com.amee.base.utils.UidGen;
import com.amee.domain.AMEEStatistics;
import com.amee.domain.APIVersion;
import com.amee.domain.Pager;
import com.amee.domain.auth.User;
import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemValue;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.environment.Environment;
import com.amee.domain.profile.Profile;
import com.amee.domain.profile.ProfileItem;
import com.amee.domain.sheet.Sheet;
import com.amee.platform.science.StartEndDate;
import com.amee.service.BaseService;
import com.amee.service.auth.PermissionService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Primary service interface for Profile Resources.
 * <p/>
 * This file is part of AMEE.
 * <p/>
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
@Service
public class ProfileService extends BaseService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProfileServiceDAO dao;

    @Autowired
    private ProfileSheetService profileSheetService;

    @Autowired
    private OnlyActiveProfileService onlyActiveProfileService;

    @Autowired
    private AMEEStatistics ameeStatistics;

    // Profiles

    /**
     * Fetches a Profile based on the supplied path. If the path is a valid UID format then the
     * Profile with this UID is returned. If a profile with the UID is not found or the path is
     * not a valid UID format then a Profile with the matching path is searched for and returned.
     *
     * @param environment that requested Profile belongs to
     * @param path        to search for. Can be either a UID or a path alias.
     * @return the matching Profile
     */
    public Profile getProfile(Environment environment, String path) {
        Profile profile = null;
        if (!StringUtils.isBlank(path)) {
            if (UidGen.INSTANCE_12.isValid(path)) {
                profile = getProfileByUid(environment, path);
            }
            if (profile == null) {
                profile = getProfileByPath(environment, path);
            }
        }
        return profile;
    }

    public Profile getProfileByUid(Environment environment, String uid) {
        Profile profile = dao.getProfileByUid(uid);
        checkEnvironmentObject(environment, profile);
        return profile;
    }

    public Profile getProfileByPath(Environment environment, String path) {
        return dao.getProfileByPath(environment, path);
    }

    public List<Profile> getProfiles(User user, Pager pager) {
        return dao.getProfiles(user, pager);
    }

    public void persist(Profile p) {
        dao.persist(p);
    }

    public void remove(Profile profile) {
        dao.remove(profile);
    }

    public void clearCaches(Profile profile) {
        log.debug("clearCaches()");
        profileSheetService.removeSheets(profile);
    }

    // ProfileItems

    public ProfileItem getProfileItem(String uid) {
        ProfileItem pi = checkProfileItem(dao.getProfileItem(uid));
        // If this ProfileItem is trashed then return null. A ProfileItem may be trash if it itself has been
        // trashed or an owning entity has been trashed.
        if (pi != null && !pi.isTrash()) {
            return pi;
        } else {
            return null;
        }
    }

    /**
     * Retrieve a list of {@link ProfileItem}s belonging to a {@link Profile} and {@link DataCategory}
     * occuring on or immediately proceeding the given date context.
     *
     * @param profile      - the {@link Profile} to which the {@link ProfileItem}s belong
     * @param dataCategory - the DataCategory containing the ProfileItems
     * @param date         - the date context
     * @return the active {@link ProfileItem} collection
     */
    public List<ProfileItem> getProfileItems(Profile profile, DataCategory dataCategory, Date date) {
        return checkProfileItems(onlyActiveProfileService.getProfileItems(dao.getProfileItems(profile, dataCategory, date)));
    }

    /**
     * Retrieve a list of {@link ProfileItem}s belonging to a {@link Profile} and {@link DataCategory}
     * occuring between a given date context.
     *
     * @param profile      - the {@link Profile} to which the {@link ProfileItem}s belong
     * @param dataCategory - the DataCategory containing the ProfileItems
     * @param startDate    - the start of the date context
     * @param endDate      - the end of the date context
     * @return the active {@link ProfileItem} collection
     */
    public List<ProfileItem> getProfileItems(
            Profile profile,
            DataCategory dataCategory,
            StartEndDate startDate,
            StartEndDate endDate) {
        return checkProfileItems(dao.getProfileItems(profile, dataCategory, startDate, endDate));
    }

    /**
     * Get a count of all {@link ProfileItem}s belonging to a {@link Profile} with a particular {@link DataCategory}.
     *
     * @param profile      - the {@link Profile} to which the {@link ProfileItem}s belong
     * @param dataCategory - the DataCategory containing the ProfileItems
     * @return the number of {@link ProfileItem}s
     */
    public int getProfileItemCount(Profile profile, DataCategory dataCategory) {
        return dao.getProfileItemCount(profile, dataCategory);
    }

    private List<ProfileItem> checkProfileItems(List<ProfileItem> profileItems) {
        if (log.isDebugEnabled()) {
            log.debug("checkProfileItems() start");
        }
        if (profileItems == null) {
            return null;
        }
        List<ProfileItem> activeProfileItems = new ArrayList<ProfileItem>();

        // Remove any trashed ProfileItems
        for (ProfileItem profileItem : profileItems) {
            if (!profileItem.isTrash()) {
                checkProfileItem(profileItem);
                activeProfileItems.add(profileItem);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("checkProfileItems() done (" + activeProfileItems.size() + ")");
        }
        return activeProfileItems;
    }

    /**
     * Add to the {@link com.amee.domain.profile.ProfileItem} any {@link com.amee.domain.data.ItemValue}s it is missing.
     * This will be the case on first persist (this method acting as a reification function), and between GETs if any
     * new {@link com.amee.domain.data.ItemValueDefinition}s have been added to the underlying
     * {@link com.amee.domain.data.ItemDefinition}.
     * <p/>
     * Any updates to the {@link com.amee.domain.profile.ProfileItem} will be persisted to the database.
     *
     * @param profileItem to check
     * @return the supplied ProfileItem or null
     */
    private ProfileItem checkProfileItem(ProfileItem profileItem) {

        if (profileItem == null) {
            return null;
        }

        // Get APIVersion via Profile & User.
        APIVersion apiVersion = profileItem.getProfile().getUser().getAPIVersion();

        // APIVersion apiVersion = profileItem.getProfile().getAPIVersion();
        Set<ItemValueDefinition> existingItemValueDefinitions = profileItem.getItemValueDefinitions();
        Set<ItemValueDefinition> missingItemValueDefinitions = new HashSet<ItemValueDefinition>();

        // find ItemValueDefinitions not currently implemented in this Item
        for (ItemValueDefinition ivd : profileItem.getItemDefinition().getItemValueDefinitions()) {
            if (ivd.isFromProfile() && ivd.getAPIVersions().contains(apiVersion)) {
                if (!existingItemValueDefinitions.contains(ivd)) {
                    missingItemValueDefinitions.add(ivd);
                }
            }
        }

        // Do we need to add any ItemValueDefinitions?
        if (missingItemValueDefinitions.size() > 0) {

            // Ensure a transaction has been opened. The implementation of open-session-in-view we are using
            // does not open transactions for GETs. This method is called for certain GETs.
            transactionController.begin(true);

            // create missing ItemValues
            for (ItemValueDefinition ivd : missingItemValueDefinitions) {
                // start default value with value from ItemValueDefinition
                String defaultValue = ivd.getValue();
                // next give DataItem a chance to set the default value, if appropriate
                if (ivd.isFromData()) {
                    ItemValue dataItemValue =
                            profileItem.getDataItem().getItemValue(ivd.getPath(), profileItem.getStartDate());
                    if ((dataItemValue != null) && (dataItemValue.getValue().length() > 0)) {
                        defaultValue = dataItemValue.getValue();
                    }
                }
                // create missing ItemValue
                new ItemValue(ivd, profileItem, defaultValue);
                ameeStatistics.createProfileItemValue();
            }
        }

        return profileItem;
    }

    public boolean isUnique(ProfileItem pi) {
        return !dao.equivilentProfileItemExists(pi);
    }

    public void persist(ProfileItem pi) {
        dao.persist(pi);
        checkProfileItem(pi);
    }

    public void remove(ProfileItem pi) {
        dao.remove(pi);
    }

    // Profile DataCategories

    public Collection<Long> getProfileDataCategoryIds(Profile profile) {
        return dao.getProfileDataCategoryIds(profile);
    }

    // Sheets

    public Sheet getSheet(CacheableFactory sheetFactory) {
        return profileSheetService.getSheet(sheetFactory);
    }
}