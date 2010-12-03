package com.amee.service.tag;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.tag.EntityTag;
import com.amee.domain.tag.Tag;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.*;

@Repository
public class TagServiceDAO implements Serializable {

    @PersistenceContext
    private EntityManager entityManager;

    protected Tag getTagByUid(String uid) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
        criteria.setTimeout(5);
        return (Tag) criteria.uniqueResult();
    }

    protected Tag getTagByTag(String tag) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.ilike("tag", tag, MatchMode.EXACT));
        criteria.setTimeout(5);
        return (Tag) criteria.uniqueResult();
    }

    @SuppressWarnings(value = "unchecked")
    protected List<Tag> getTags() {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setTimeout(5);
        return criteria.list();
    }

    @SuppressWarnings(value = "unchecked")
    protected List<Tag> getTagsWithCount() {
        // Create Query.
        Query query = entityManager.createQuery(
                "SELECT distinct t, count(et) " +
                        "FROM Tag t " +
                        "LEFT JOIN t.entityTags et " +
                        "WHERE t.status != :trash " +
                        "AND et.status != :trash " +
                        "GROUP BY t.tag, t.uid " +
                        "ORDER BY t.tag");
        query.setParameter("trash", AMEEStatus.TRASH);
        query.setHint("org.hibernate.timeout", 5);
        // Collate Tags and set count value.
        List<Object> results = query.getResultList();
        List<Tag> tags = new ArrayList<Tag>();
        for (Object o : results) {
            Object[] result = (Object[]) o;
            Tag tag = (Tag) result[0];
            tag.setCount((Long) result[1]);
            tags.add(tag);
        }
        return tags;
    }

    @SuppressWarnings(value = "unchecked")
    public List<EntityTag> getEntityTags(IAMEEEntityReference entity) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(EntityTag.class);
        criteria.add(Restrictions.eq("entityReference.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entityReference.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setFetchMode("tag", FetchMode.JOIN);
        criteria.setTimeout(5);
        return criteria.list();
    }

    @SuppressWarnings(value = "unchecked")
    public List<EntityTag> getEntityTags(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
        Set<Long> entityIds = new HashSet<Long>();
        entityIds.add(0L);
        for (IAMEEEntityReference entity : entities) {
            entityIds.add(entity.getEntityId());
        }
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(EntityTag.class);
        criteria.add(Restrictions.in("entityReference.entityId", entityIds));
        criteria.add(Restrictions.eq("entityReference.entityType", objectType.getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setFetchMode("tag", FetchMode.JOIN);
        criteria.setTimeout(5);
        return criteria.list();
    }

    public EntityTag getEntityTag(IAMEEEntityReference entity, String tag) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(EntityTag.class);
        criteria.createAlias("tag", "t");
        criteria.add(Restrictions.eq("entityReference.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entityReference.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.ilike("t.tag", tag, MatchMode.EXACT));
        criteria.setTimeout(5);
        return (EntityTag) criteria.uniqueResult();
    }

    public void persist(Tag tag) {
        entityManager.persist(tag);
    }

    public void remove(Tag tag) {
        tag.setStatus(AMEEStatus.TRASH);
    }

    public void persist(EntityTag entityTag) {
        entityManager.persist(entityTag);
    }

    public void remove(EntityTag entityTag) {
        entityTag.setStatus(AMEEStatus.TRASH);
    }
}