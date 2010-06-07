package com.amee.service.locale;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.data.LocaleName;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class LocaleServiceDAO {

    private static final String CACHE_REGION = "query.localeService";

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings(value = "unchecked")
    public List<LocaleName> getLocaleNames(IAMEEEntityReference entity) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(LocaleName.class);
        criteria.add(Restrictions.eq("entity.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entity.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        return criteria.list();
    }

    /**
     * Note: This can return LocaleNames associated with various types of entities.
     *
     * @param objectType
     * @param entities
     * @return
     */
    @SuppressWarnings(value = "unchecked")
    public List<LocaleName> getLocaleNames(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        Set<Long> entityIds = new HashSet<Long>();
        entityIds.add(0L);
        for (IAMEEEntityReference entity : entities) {
            entityIds.add(entity.getEntityId());
        }
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(LocaleName.class);
        criteria.add(Restrictions.in("entity.entityId", entityIds));
        criteria.add(Restrictions.eq("entity.entityType", objectType.getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    protected void persist(LocaleName localeName) {
        entityManager.persist(localeName);
    }

    protected void remove(LocaleName localeName) {
        localeName.setStatus(AMEEStatus.TRASH);
    }
}
