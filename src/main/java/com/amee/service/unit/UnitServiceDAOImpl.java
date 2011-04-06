package com.amee.service.unit;

import com.amee.domain.AMEEStatus;
import com.amee.domain.unit.AMEEUnitType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class UnitServiceDAOImpl implements UnitServiceDAO {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Fetch a list of all AMEEUnitTypes.
     *
     * @return list of all AMEEUnitTypes
     */
    public List<AMEEUnitType> getUnitTypes() {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(AMEEUnitType.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.addOrder(Order.asc(StringUtils.lowerCase("name")));
        criteria.setTimeout(60);
        return criteria.list();
    }

    /**
     * Fetch an AMEEUnitType with the given UID value.
     *
     * @param uid value to match AMEEUnitType on
     * @return AMEEUnitType matching the name value
     */
    @Override
    public AMEEUnitType getUnitTypeByUid(String uid) {
        if (StringUtils.isNotBlank(uid)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnitType.class);
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            criteria.setTimeout(60);
            return (AMEEUnitType) criteria.uniqueResult();
        } else {
            return null;
        }
    }

    /**
     * Fetch an AMEEUnitType with the given name value.
     * <p/>
     * This query uses FlushMode.MANUAL to ensure the session is not flushed prior to execution.
     *
     * @param name value to match AMEEUnitType on
     * @return AMEEUnitType matching the name value
     */
    @Override
    public AMEEUnitType getUnitTypeByName(String name) {
        if (StringUtils.isNotBlank(name)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnitType.class);
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.add(Restrictions.ilike("name", name, MatchMode.EXACT));
            criteria.setFlushMode(FlushMode.MANUAL);
            criteria.setTimeout(60);
            return (AMEEUnitType) criteria.uniqueResult();
        } else {
            return null;
        }
    }

    /**
     * Returns true if the name of the supplied UnitType is unique.
     *
     * @param unitType to check for uniqueness
     * @return true if the UnitType has a unique name
     */
    public boolean isUnitTypeUniqueByName(AMEEUnitType unitType) {
        if (unitType != null) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnitType.class);
            if (entityManager.contains(unitType)) {
                criteria.add(Restrictions.ne("uid", unitType.getUid()));
            }
            criteria.add(Restrictions.ilike("name", unitType.getName(), MatchMode.EXACT));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFlushMode(FlushMode.MANUAL);
            criteria.setTimeout(60);
            return criteria.list().isEmpty();
        } else {
            throw new RuntimeException("UnitType was null.");
        }
    }

    public void persist(AMEEUnitType unitType) {
        entityManager.persist(unitType);
    }
}
