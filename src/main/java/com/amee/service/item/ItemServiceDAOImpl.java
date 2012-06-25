package com.amee.service.item;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.BaseDataItemValue;
import com.amee.domain.item.data.DataItemTextValue;
import com.amee.domain.item.profile.BaseProfileItemValue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ItemServiceDAOImpl implements ItemServiceDAO {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String CACHE_REGION = "query.itemService";

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Returns the Class for the Item implementation.
     *
     * @return the entity Class
     */
    public abstract Class getEntityClass();

    /**
     * Returns the Item matching the specified UID.
     *
     * @param uid for the requested Item
     * @return the matching Item or null if not found
     */
    @SuppressWarnings(value = "unchecked")
    public BaseItem getItemByUid(String uid) {
        BaseItem item = null;
        if (!StringUtils.isBlank(uid)) {
            // See http://www.hibernate.org/117.html#A12 for notes on DISTINCT_ROOT_ENTITY.
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(getEntityClass());
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<BaseItem> items = criteria.list();
            if (items.size() == 1) {
                item = items.get(0);
            } else {
                log.debug("getItemByUid() NOT found: {}", uid);
            }
        }
        return item;
    }

    public void persist(BaseItemValue itemValue) {
        entityManager.persist(itemValue);
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param items
     * @param kls
     * @return
     */
    @SuppressWarnings(value = "unchecked")
    public List<DataItemTextValue> getItemValuesForItems(Collection<BaseItem> items, Class kls) {
        Set<Long> entityIds = new HashSet<Long>();
        entityIds.add(0L);
        for (IAMEEEntityReference entity : items) {
            // TODO: Could optimise here by excluding entities that don't match the supplied class.
            entityIds.add(entity.getEntityId());
        }
        String propertyName;
        if (BaseDataItemValue.class.isAssignableFrom(kls)) {
            propertyName = "dataItem.id";
        } else if (BaseProfileItemValue.class.isAssignableFrom(kls)) {
            propertyName = "profileItem.id";
        } else {
            throw new IllegalStateException("Instancs of BaseDataItemValue or BaseProfileItemValue were expected.");
        }
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(kls);
        criteria.add(Restrictions.in(propertyName, entityIds));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }
}
