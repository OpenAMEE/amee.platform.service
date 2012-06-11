package com.amee.service.auth;

import com.amee.domain.*;
import com.amee.domain.auth.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class PermissionServiceDAOImpl implements PermissionServiceDAO {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CACHE_REGION = "query.permissionService";

    @PersistenceContext
    private EntityManager entityManager;

    public Permission getPermissionByUid(String uid) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Permission.class);
        criteria.add(Restrictions.naturalId().set("uid", uid));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        return (Permission) criteria.uniqueResult();
    }

    public void persist(Permission permission) {
        entityManager.persist(permission);
    }

    public void remove(Permission permission) {
        permission.setStatus(AMEEStatus.TRASH);
    }

    @SuppressWarnings(value = "unchecked")
    public List<Permission> getPermissionsForEntity(IAMEEEntityReference entity) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Permission.class);
        criteria.add(Restrictions.eq("entityReference.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entityReference.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        return criteria.list();
    }

    @SuppressWarnings(value = "unchecked")
    public List<Permission> getPermissionsForPrincipal(IAMEEEntityReference principal, Class entityClass) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Permission.class);
        criteria.add(Restrictions.eq("principalReference.entityUid", principal.getEntityUid()));
        criteria.add(Restrictions.eq("principalReference.entityType", principal.getObjectType().getName()));
        if (entityClass != null) {
            criteria.add(Restrictions.eq("entityReference.entityType", ObjectType.fromClass(entityClass).getName()));
        }
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        return criteria.list();
    }

    @SuppressWarnings(value = "unchecked")
    public List<Permission> getPermissionsForEntity(IAMEEEntityReference principal, IAMEEEntityReference entity) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Permission.class);
        criteria.add(Restrictions.eq("principalReference.entityUid", principal.getEntityUid()));
        criteria.add(Restrictions.eq("principalReference.entityType", principal.getObjectType().getName()));
        criteria.add(Restrictions.eq("entityReference.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entityReference.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        return criteria.list();
    }

    @SuppressWarnings(value = "unchecked")
    public IAMEEEntity getEntity(IAMEEEntityReference entityReference) {
        if (entityReference == null) {
            throw new IllegalArgumentException();
        }
        if (entityReference.getEntityId() != null) {
            log.debug("getEntity() - using entityManager.find()");
            return (AMEEEntity) entityManager.find(
                    entityReference.getObjectType().toClass(), entityReference.getEntityId());
        } else {
            log.debug("getEntity() - using query");
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(entityReference.getObjectType().toClass());
            criteria.add(Restrictions.naturalId().set("uid", entityReference.getEntityUid()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            return (IAMEEEntity) criteria.uniqueResult();
        }
    }

    public void trashPermissionsForEntity(IAMEEEntityReference entity) {
        entityManager.createQuery(
                "UPDATE Permission " +
                        "SET status = :trash, " +
                        "modified = current_timestamp() " +
                        "WHERE entityReference.entityUid = :entityUid " +
                        "AND entityReference.entityType = :entityType " +
                        "AND status != :trash")
                .setParameter("trash", AMEEStatus.TRASH)
                .setParameter("entityUid", entity.getEntityUid())
                .setParameter("entityType", entity.getObjectType().getName())
                .executeUpdate();
    }
}
