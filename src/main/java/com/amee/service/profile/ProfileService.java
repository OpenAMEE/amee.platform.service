package com.amee.service.profile;

import com.amee.base.transaction.TransactionController;
import com.amee.base.utils.UidGen;
import com.amee.domain.AMEEStatistics;
import com.amee.domain.APIVersion;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.Pager;
import com.amee.domain.auth.User;
import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemValue;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.item.profile.NuProfileItem;
import com.amee.domain.profile.Profile;
import com.amee.domain.profile.ProfileItem;
import com.amee.domain.sheet.Sheet;
import com.amee.platform.science.StartEndDate;
import com.amee.service.BaseService;
import com.amee.service.auth.PermissionService;
import com.amee.service.item.ProfileItemService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
    private ProfileItemService profileItemService;

    @Autowired
    private AMEEStatistics ameeStatistics;

    // Profiles

    /**
     * Fetches a Profile based on the supplied path. If the path is a valid UID format then the
     * Profile with this UID is returned. If a profile with the UID is not found or the path is
     * not a valid UID format then a Profile with the matching path is searched for and returned.
     *
     * @param path to search for. Can be either a UID or a path alias.
     * @return the matching Profile
     */
    public Profile getProfile(String path) {
        Profile profile = null;
        if (!StringUtils.isBlank(path)) {
            if (UidGen.INSTANCE_12.isValid(path)) {
                profile = getProfileByUid(path);
            }
            if (profile == null) {
                profile = getProfileByPath(path);
            }
        }
        return profile;
    }

    public Profile getProfileByUid(String uid) {
        return dao.getProfileByUid(uid);
    }

    public Profile getProfileByPath(String path) {
        return dao.getProfileByPath(path);
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
        ProfileItem pi = ProfileItem.getProfileItem(profileItemService.getItemByUid(uid));
        if (pi == null) {
            pi = dao.getProfileItem(uid);
        }
        pi = checkProfileItem(pi);
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
    public List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, Date date) {
        Set<String> profileItemUids = new HashSet<String>();
        List<ProfileItem> profileItems = new ArrayList<ProfileItem>();
        for (NuProfileItem profileItem : profileItemService.getProfileItems(profile, dataCategory, date)) {
            profileItemUids.add(profileItem.getUid());
            profileItems.add(ProfileItem.getProfileItem(profileItem));
        }
        for (ProfileItem profileItem : dao.getProfileItems(profile, dataCategory, date)) {
            if (!profileItemUids.contains(profileItem.getUid())) {
                profileItems.add(profileItem);
            }
        }
        // Order the returned collection by pi.name, di.name and pi.startDate DESC
        Collections.sort(profileItems, new Comparator<ProfileItem>() {
            public int compare(ProfileItem p1, ProfileItem p2) {
                int nd = p1.getName().compareTo(p2.getName());
                int dnd = p1.getDataItem().getName().compareTo(p2.getDataItem().getName());
                int sdd = p2.getStartDate().compareTo(p1.getStartDate());
                if (nd != 0) return nd;
                if (dnd != 0) return dnd;
                if (sdd != 0) return sdd;
                return 0;
            }
        });
        return checkProfileItems(onlyActiveProfileService.getProfileItems(profileItems));
    }

    /**
     * Retrieve a list of {@link ProfileItem}s belonging to a {@link Profile} and {@link DataCategory}
     * occurring between a given date context.
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
        Set<String> profileItemUids = new HashSet<String>();
        List<ProfileItem> profileItems = new ArrayList<ProfileItem>();
        for (NuProfileItem profileItem : profileItemService.getProfileItems(profile, dataCategory, startDate, endDate)) {
            profileItemUids.add(profileItem.getUid());
            profileItems.add(ProfileItem.getProfileItem(profileItem));
        }
        for (ProfileItem profileItem : dao.getProfileItems(profile, dataCategory, startDate, endDate)) {
            if (!profileItemUids.contains(profileItem.getUid())) {
                profileItems.add(profileItem);
            }
        }
        // Order the returned collection by pi.startDate DESC
        Collections.sort(profileItems, new Comparator<ProfileItem>() {
            public int compare(ProfileItem p1, ProfileItem p2) {
                return p2.getStartDate().compareTo(p1.getStartDate());
            }
        });
        return checkProfileItems(profileItems);
    }

    /**
     * Get a count of all {@link ProfileItem}s belonging to a {@link Profile} with a particular {@link DataCategory}.
     *
     * @param profile      - the {@link Profile} to which the {@link ProfileItem}s belong
     * @param dataCategory - the DataCategory containing the ProfileItems
     * @return the number of {@link ProfileItem}s
     */
    public int getProfileItemCount(Profile profile, DataCategory dataCategory) {
        return dao.getProfileItemCount(profile, dataCategory) +
                profileItemService.getProfileItemCount(profile, dataCategory);
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
                persist(new ItemValue(ivd, profileItem, defaultValue));
                ameeStatistics.createProfileItemValue();
            }

            // Clear cache.
            profileItemService.clearItemValues();
        }

        return profileItem;
    }

    public boolean isUnique(ProfileItem pi) {
        return !dao.equivalentProfileItemExists(pi) && !profileItemService.equivalentProfileItemExists(pi);
    }

    public void persist(ProfileItem profileItem) {
        if (profileItem.isLegacy()) {
            dao.persist(profileItem);
        } else {
            profileItemService.persist(profileItem.getNuEntity());
        }
        checkProfileItem(profileItem);
    }

    public void remove(ProfileItem pi) {
        dao.remove(pi);
    }

    // Item Values.

    public void persist(ItemValue itemValue) {
        if (!itemValue.isLegacy()) {
            profileItemService.persist(itemValue.getNuEntity());
        }
    }

    // Profile DataCategories

    public Set<Long> getProfileDataCategoryIds(Profile profile) {
        Set<Long> dataCategoryIds = new HashSet<Long>();
        dataCategoryIds.addAll(dao.getProfileDataCategoryIds(profile));
        dataCategoryIds.addAll(profileItemService.getProfileDataCategoryIds(profile));
        return dataCategoryIds;
    }

    // Sheets

    public Sheet getSheet(CacheableFactory sheetFactory) {
        return profileSheetService.getSheet(sheetFactory);
    }

    // Nu to Adapter.

    /**
     * Convert from legacy to adapter.
     *
     * @param profileItems
     * @return
     */
    public List<ProfileItem> getProfileItems(List<NuProfileItem> profileItems) {
        List<ProfileItem> adapters = new ArrayList<ProfileItem>();
        for (NuProfileItem profileItem : profileItems) {
            adapters.add(ProfileItem.getProfileItem(profileItem));
        }
        return adapters;
    }
}