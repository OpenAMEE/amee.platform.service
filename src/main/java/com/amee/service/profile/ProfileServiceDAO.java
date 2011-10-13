package com.amee.service.profile;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.AMEEStatus;
import com.amee.domain.Pager;
import com.amee.domain.auth.User;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.profile.ProfileItem;
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
import javax.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates all persistence operations for Profiles and Profile Items.
 *
 * TODO: Extract interface?
 * TODO: Why are these methods protected?
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

    @SuppressWarnings("unchecked")
    public ResultsWrapper<Profile> getProfilesByUserUid(String uid, int resultStart, int resultLimit) {
        // Create Query, apply start and limit if relevant.
        Query query = entityManager.createQuery(
            "SELECT p FROM Profile p " +
                "WHERE p.user.uid = :userUid " +
                "AND p.status != :trash")
            .setParameter("userUid", uid)
            .setParameter("trash", AMEEStatus.TRASH)
            .setHint("org.hibernate.cacheable", true)
            .setHint("org.hibernate.cacheRegion", CACHE_REGION);
        if (resultStart > 0) {
            query.setFirstResult(resultStart);
        }
        if (resultLimit > 0) {

            // Get 1 more than result limit so we know if we have them all or there are more to fetch.
            query.setMaxResults(resultLimit + 1);
        }

        // Get the results
        List<Profile> profiles = (List<Profile>) query.getResultList();

        // Did we limit the results?
        if (resultLimit > 0) {

            // Results were limited, work out correct results and truncation state.
            return new ResultsWrapper<Profile>(
                profiles.size() > resultLimit ? profiles.subList(0, resultLimit) : profiles,
                profiles.size() > resultLimit);
        } else {

            // Results were not limited, no truncation
            return new ResultsWrapper<Profile>(profiles, false);
        }
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

    public Set<DataCategory> getProfileDataCategories(Profile profile) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItem.class);
        criteria.add(Restrictions.eq("profile.id", profile.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        List<ProfileItem> profileItems = criteria.list();
        Set<DataCategory> dataCategories = new HashSet<DataCategory>();
        for (ProfileItem item : profileItems) {
            dataCategories.add(item.getDataCategory());
        }
        return dataCategories;
    }
}