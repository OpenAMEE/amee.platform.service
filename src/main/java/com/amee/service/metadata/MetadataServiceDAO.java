package com.amee.service.metadata;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.Metadata;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;

@Repository
public class MetadataServiceDAO implements Serializable {

    private static final String CACHE_REGION = "query.metadataService";

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings(value = "unchecked")
    public Metadata getMetadataForEntity(IAMEEEntityReference entity, String name) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Metadata.class);
        criteria.add(Restrictions.eq("entityReference.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entityReference.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.eq("name", name));
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        try {
            return (Metadata) criteria.uniqueResult();
        } catch (HibernateException e) {
            return null;
        }
    }

    public void persist(Metadata metadata) {
        entityManager.persist(metadata);
    }

    public void remove(Metadata metadata) {
        metadata.setStatus(AMEEStatus.TRASH);
    }
}
