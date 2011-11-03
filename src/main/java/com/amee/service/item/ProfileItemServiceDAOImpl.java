package com.amee.service.item;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.AMEEStatus;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.ProfileItemsFilter;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.profile.ProfileItem;
import com.amee.domain.item.profile.ProfileItemNumberValue;
import com.amee.domain.item.profile.ProfileItemTextValue;
import com.amee.domain.profile.Profile;
import com.amee.platform.science.StartEndDate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ProfileItemServiceDAOImpl extends ItemServiceDAOImpl implements ProfileItemServiceDAO {

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public Class getEntityClass() {
        return ProfileItem.class;
    }

    /**
     * Returns the ProfileItem matching the specified UID.
     *
     * @param uid for the requested ProfileItem
     * @return the matching ProfileItem or null if not found
     */
    @Override
    public ProfileItem getItemByUid(String uid) {
        return (ProfileItem) super.getItemByUid(uid);
    }

    // ItemValues.

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        if (!ProfileItem.class.isAssignableFrom(item.getClass())) throw new IllegalStateException();
        return getProfileItemValues((ProfileItem) item);
    }

    @Override
    public Set<BaseItemValue> getProfileItemValues(ProfileItem profileItem) {
        Set<BaseItemValue> rawItemValues = new HashSet<BaseItemValue>();
        rawItemValues.addAll(getProfileItemNumberValues(profileItem));
        rawItemValues.addAll(getProfileItemTextValues(profileItem));
        return rawItemValues;
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param profileItem
     * @return
     */
    public List<ProfileItemNumberValue> getProfileItemNumberValues(ProfileItem profileItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItemNumberValue.class);
        criteria.add(Restrictions.eq("profileItem.id", profileItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param profileItem
     * @return
     */
    @Override
    @SuppressWarnings(value = "unchecked")
    public List<ProfileItemTextValue> getProfileItemTextValues(ProfileItem profileItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItemTextValue.class);
        criteria.add(Restrictions.eq("profileItem.id", profileItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    @Override
    public Set<BaseItemValue> getItemValuesForItems(Collection<BaseItem> items) {
        Set<BaseItemValue> itemValues = new HashSet<BaseItemValue>();
        itemValues.addAll(getItemValuesForItems(items, ProfileItemNumberValue.class));
        itemValues.addAll(getItemValuesForItems(items, ProfileItemTextValue.class));
        return itemValues;
    }

    @Override
    public int getProfileItemCount(Profile profile, DataCategory dataCategory) {

        if ((dataCategory == null) || (dataCategory.getItemDefinition() == null)) {
            return -1;
        }

        log.debug("getProfileItemCount() start");

        // Get the ProfileItems count
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItem.class);
        criteria.add(Restrictions.eq("dataCategory.id", dataCategory.getId()));
        criteria.add(Restrictions.eq("profile.id", profile.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        int count = criteria.list().size();

        log.debug("getProfileItemCount() count: " + count);

        return count;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResultsWrapper<ProfileItem> getProfileItems(Profile profile, ProfileItemsFilter filter) {
        log.debug("getProfileItems() start");

        // Get the ProfileItems (sorted in creation order)
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItem.class);
        criteria.add(Restrictions.eq("profile.id", profile.getId()));
        if (filter.getEndDate() != null) {
            criteria.add(Restrictions.lt("startDate", filter.getEndDate()));
        }
        criteria.add(Restrictions.or(Restrictions.isNull("endDate"), Restrictions.gt("endDate", filter.getStartDate())));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.addOrder(Order.asc("startDate"));
        if (filter.getResultStart() > 0) {
            criteria.setFirstResult(filter.getResultStart());
        }
        if (filter.getResultLimit() > 0) {
            // Get 1 more than result limit so we know if we have them all or there are more to fetch.
            criteria.setMaxResults(filter.getResultLimit() + 1);
        }
        List<ProfileItem> profileItems = criteria.list();

        if (log.isDebugEnabled()) {
            log.debug("getProfileItems() done (" + profileItems.size() + ")");
        }

        // Did we limit the results?
        if (filter.getResultLimit() > 0) {

            // Results were limited, work out correct results and truncation state.
            return new ResultsWrapper<ProfileItem>(
                profileItems.size() > filter.getResultLimit() ? profileItems.subList(0, filter.getResultLimit()) :
                    profileItems, profileItems.size() > filter.getResultLimit());
        } else {

            // Results were not limited, no truncation
            return new ResultsWrapper<ProfileItem>(profileItems, false);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, Date profileDate) {

        if ((dataCategory == null) || (!dataCategory.isItemDefinitionPresent())) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("getProfileItems() start");
        }

        // Need to roll the date forward.
        DateTime nextMonth = new DateTime(profileDate).plus(Period.months(1));
        profileDate = nextMonth.toDate();

        // Get the ProfileItems.
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItem.class);
        criteria.add(Restrictions.eq("dataCategory.id", dataCategory.getEntityId()));
        criteria.add(Restrictions.eq("profile.id", profile.getId()));
        criteria.add(Restrictions.lt("startDate", profileDate));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        List<ProfileItem> profileItems = criteria.list();

        if (log.isDebugEnabled()) {
            log.debug("getProfileItems() done (" + profileItems.size() + ")");
        }

        return profileItems;
    }

    @Override
    public List<ProfileItem> getProfileItems(Profile profile, IDataCategoryReference dataCategory, StartEndDate startDate, StartEndDate endDate) {

        if ((dataCategory == null) || (!dataCategory.isItemDefinitionPresent())) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("getProfileItems() start");
        }

        // Get the ProfileItems.
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItem.class);
        criteria.add(Restrictions.eq("dataCategory.id", dataCategory.getEntityId()));
        criteria.add(Restrictions.eq("profile.id", profile.getId()));
        if (endDate != null) {
            criteria.add(Restrictions.lt("startDate", endDate.toDate()));
        }
        criteria.add(Restrictions.or(Restrictions.isNull("endDate"), Restrictions.gt("endDate", startDate.toDate())));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        List<ProfileItem> profileItems = criteria.list();

        if (log.isDebugEnabled()) {
            log.debug("getProfileItems() done (" + profileItems.size() + ")");
        }

        return profileItems;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public boolean equivalentProfileItemExists(ProfileItem profileItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItem.class);
        criteria.add(Restrictions.eq("profile.id", profileItem.getProfile().getId()));
        criteria.add(Restrictions.ne("uid", profileItem.getUid()));
        criteria.add(Restrictions.eq("dataCategory.id", profileItem.getDataCategory().getId()));
        criteria.add(Restrictions.eq("dataItem.id", profileItem.getDataItem().getId()));
        criteria.add(Restrictions.eq("startDate", profileItem.getStartDate()));
        criteria.add(Restrictions.eq("name", profileItem.getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        List<ProfileItem> profileItems = criteria.list();
        if (profileItems.size() > 0) {
            log.debug("equivalentProfileItemExists() - found ProfileItem(s)");
            return true;
        } else {
            log.debug("equivalentProfileItemExists() - no ProfileItem(s) found");
            return false;
        }
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Collection<Long> getProfileDataCategoryIds(Profile profile) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(ProfileItem.class);
        criteria.add(Restrictions.eq("profile.id", profile.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        List<ProfileItem> profileItems = criteria.list();
        List<Long> ids = new ArrayList<Long>();
        for (ProfileItem item : profileItems) {
            ids.add(item.getDataCategory().getId());
        }
        return ids;
    }

    @Override
    public void persist(ProfileItem profileItem) {
        entityManager.persist(profileItem);
    }
}