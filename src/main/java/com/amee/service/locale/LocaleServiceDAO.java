package com.amee.service.locale;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.data.LocaleName;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

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

    protected void persist(LocaleName localeName) {
        entityManager.persist(localeName);
    }

    protected void remove(LocaleName localeName) {
        localeName.setStatus(AMEEStatus.TRASH);
    }
}
