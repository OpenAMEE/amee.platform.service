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
package com.amee.service.profile;

import com.amee.domain.AMEEStatus;
import com.amee.domain.Pager;
import com.amee.domain.auth.User;
import com.amee.domain.profile.Profile;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Encapsulates all persistence operations for Profiles and Profile Items.
 */
@Service
public class ProfileServiceDAO {

    private final Log log = LogFactory.getLog(getClass());

    private static final String CACHE_REGION = "query.profileService";

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings(value = "unchecked")
    protected Profile getProfileByUid(String uid) {
        Profile profile = null;
        if (!StringUtils.isBlank(uid)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(Profile.class);
            criteria.add(Restrictions.naturalId().set("uid", uid));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<Profile> profiles = criteria.list();
            if (profiles.size() == 1) {
                log.debug("getProfileByUid() found: " + uid);
                profile = profiles.get(0);
            } else {
                log.debug("getProfileByUid() NOT found: " + uid);
            }
        }
        return profile;
    }

    @SuppressWarnings(value = "unchecked")
    protected List<Profile> getProfiles(User user, Pager pager) {
        // first count all profiles
        long count = (Long) entityManager.createQuery(
                "SELECT count(p) " +
                        "FROM Profile p " +
                        "WHERE p.user.id = :userId " +
                        "AND p.status != :trash")
                .setParameter("userId", user.getId())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getSingleResult();
        // tell pager how many profiles there are and give it a chance to select the requested page again
        pager.setItems(count);
        pager.goRequestedPage();
        // now get the profiles for the current page
        List<Profile> profiles = entityManager.createQuery(
                "SELECT p " +
                        "FROM Profile p " +
                        "WHERE p.user.id = :userId " +
                        "AND p.status != :trash " +
                        "ORDER BY p.created DESC")
                .setParameter("userId", user.getId())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .setMaxResults(pager.getItemsPerPage())
                .setFirstResult((int) pager.getStart())
                .getResultList();
        // update the pager
        pager.setItemsFound(profiles.size());
        // all done, return results
        return profiles;
    }

    protected void persist(Profile profile) {
        entityManager.persist(profile);
    }

    /**
     * Removes (trashes) a Profile.
     *
     * @param profile to remove
     */
    protected void remove(Profile profile) {
        profile.setStatus(AMEEStatus.TRASH);
    }
}