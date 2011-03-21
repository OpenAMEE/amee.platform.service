package com.amee.service.profile;

import com.amee.base.utils.UidGen;
import com.amee.domain.IProfileItemService;
import com.amee.domain.Pager;
import com.amee.domain.auth.User;
import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.profile.Profile;
import com.amee.domain.sheet.Sheet;
import com.amee.service.data.DataService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class ProfileService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DataService dataService;

    @Autowired
    private ProfileServiceDAO dao;

    @Autowired
    private ProfileSheetService profileSheetService;

    @Autowired
    private IProfileItemService profileItemService;

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

    // Profile DataCategories

    public Set<Long> getProfileDataCategoryIds(Profile profile) {
        Set<Long> dataCategoryIds = new HashSet<Long>();
        // Get Data Category IDs for Profile Items.
        dataCategoryIds.addAll(profileItemService.getProfileDataCategoryIds(profile));
        // Get parent Data Category IDs based on existing Data Category IDs.
        dataCategoryIds.addAll(dataService.getParentDataCategoryIds(dataCategoryIds));
        return dataCategoryIds;
    }

    // Sheets

    public Sheet getSheet(CacheableFactory sheetFactory) {
        return profileSheetService.getSheet(sheetFactory);
    }
}