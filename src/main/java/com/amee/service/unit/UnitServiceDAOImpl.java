package com.amee.service.unit;

import com.amee.domain.AMEEStatus;
import com.amee.domain.unit.AMEEUnit;
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

    // Unit Types.

    /**
     * Fetch a list of all Unit Types.
     *
     * @return list of all Unit Types
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
     * Fetch a Unit Type with the given UID value.
     *
     * @param uid value to match Unit Type on
     * @return Unit Type matching the name value
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
     * Fetch a Unit Type with the given name value.
     * <p/>
     * This query uses FlushMode.MANUAL to ensure the session is not flushed prior to execution.
     *
     * @param name value to match Unit Type on
     * @return Unit Type matching the name value
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
     * Returns true if the name of the supplied Unit Type is unique.
     *
     * @param unitType to check for uniqueness
     * @return true if the Unit Type has a unique name
     */
    @Override
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

    // Units.

    /**
     * Fetch a list of all Units.
     *
     * @return list of all Units
     */
    public List<AMEEUnit> getUnits(AMEEUnitType unitType) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(AMEEUnit.class);
        if (unitType != null) {
            criteria.add(Restrictions.eq("unitType.id", unitType.getId()));
        }
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setTimeout(60);
        return criteria.list();
    }

    /**
     * Fetch a Unit with the given UID value.
     *
     * @param uid value to match Unit on
     * @return AMEEUnit matching the name value
     */
    @Override
    public AMEEUnit getUnitByUid(String uid) {
        if (StringUtils.isNotBlank(uid)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnit.class);
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            criteria.setTimeout(60);
            return (AMEEUnit) criteria.uniqueResult();
        } else {
            return null;
        }
    }

    /**
     * Fetch a Unit with the given name value.
     * <p/>
     * This query uses FlushMode.MANUAL to ensure the session is not flushed prior to execution.
     *
     * @param symbol value to match AMEEUnit on
     * @return AMEEUnit matching the name value
     */
    @Override
    public AMEEUnit getUnitBySymbol(String symbol) {
        if (StringUtils.isNotBlank(symbol)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnit.class);
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.add(
                    Restrictions.or(
                            Restrictions.eq("internalSymbol", symbol),
                            Restrictions.eq("externalSymbol", symbol)));
            criteria.setFlushMode(FlushMode.MANUAL);
            criteria.setTimeout(60);
            return (AMEEUnit) criteria.uniqueResult();
        } else {
            return null;
        }
    }

    /**
     * Returns true if the name of the supplied Unit is unique.
     *
     * @param unit to check for uniqueness
     * @return true if the Unit has a unique name
     */
    @Override
    public boolean isUnitUniqueByName(AMEEUnit unit) {
        if (unit != null) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnit.class);
            if (entityManager.contains(unit)) {
                criteria.add(Restrictions.ne("uid", unit.getUid()));
            }
            criteria.add(Restrictions.ilike("name", unit.getName(), MatchMode.EXACT));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFlushMode(FlushMode.MANUAL);
            criteria.setTimeout(60);
            return criteria.list().isEmpty();
        } else {
            throw new RuntimeException("Unit was null.");
        }
    }

    /**
     * Returns true if the internalSymbol of the supplied Unit is unique.
     *
     * @param unit to check for uniqueness
     * @return true if the Unit has a unique internalSymbol
     */
    @Override
    public boolean isUnitUniqueByInternalSymbol(AMEEUnit unit) {
        if (unit != null) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnit.class);
            if (entityManager.contains(unit)) {
                criteria.add(Restrictions.ne("uid", unit.getUid()));
            }
            criteria.add(
                    Restrictions.or(
                            Restrictions.eq("internalSymbol", unit.getInternalSymbol()),
                            Restrictions.eq("externalSymbol", unit.getInternalSymbol())));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFlushMode(FlushMode.MANUAL);
            criteria.setTimeout(60);
            return criteria.list().isEmpty();
        } else {
            throw new RuntimeException("Unit was null.");
        }
    }

    /**
     * Returns true if the externalSymbol of the supplied Unit is unique.
     *
     * @param unit to check for uniqueness
     * @return true if the Unit has a unique externalSymbol
     */
    @Override
    public boolean isUnitUniqueByExternalSymbol(AMEEUnit unit) {
        if ((unit != null) && unit.hasExternalSymbol()) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(AMEEUnit.class);
            if (entityManager.contains(unit)) {
                criteria.add(Restrictions.ne("uid", unit.getUid()));
            }
            criteria.add(
                    Restrictions.or(
                            Restrictions.eq("internalSymbol", unit.getExternalSymbol()),
                            Restrictions.eq("externalSymbol", unit.getExternalSymbol())));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFlushMode(FlushMode.MANUAL);
            criteria.setTimeout(60);
            return criteria.list().isEmpty();
        } else {
            throw new RuntimeException("Unit was null or externalSymbol was empty.");
        }
    }

    public void persist(AMEEUnit unit) {
        entityManager.persist(unit);
    }
}
