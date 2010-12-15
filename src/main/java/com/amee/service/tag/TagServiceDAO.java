package com.amee.service.tag;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.tag.EntityTag;
import com.amee.domain.tag.Tag;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
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

    /**
     * Fetch a Tag with the given tag value.
     *
     * This query uses FlushMode.MANUAL to ensure the session is not flushed prior to execution.
     *
     * @param tag value to match Tag on
     * @return Tag matching the tag value
     */
    protected Tag getTagByTag(String tag) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.ilike("tag", tag, MatchMode.EXACT));
        criteria.setTimeout(5);
        criteria.setFlushMode(FlushMode.MANUAL);
        return (Tag) criteria.uniqueResult();
    }

    @SuppressWarnings(value = "unchecked")
    protected List<Tag> getTagsWithCount() {
        return getTagsWithCount(null, null);
    }

    /**
     * Returns a List of Tags with their count value set. The count represents the number of entities that
     * have been 'tagged' with the Tag. If the incTags parameter is supplied then only Tags which have been
     * applied to entities that also have incTags Tags applied are returned. If the excTags parameter is
     * supplied then only Tags which have NOT been applied to entities that have excTags Tags applied are returned.
     * <p/>
     * TODO: This method will need to be updated if Tags are used on entities other than just Data Categories.
     *
     * @param incTags Tags
     * @param excTags
     * @return
     */
    @SuppressWarnings(value = "unchecked")
    protected List<Tag> getTagsWithCount(Collection<String> incTags, Collection<String> excTags) {
        // Get Entity IDs for Tags to include.
        Collection<EntityTag> incEntityTags = getEntityTagsForTags(ObjectType.DC, incTags);
        Collection<Long> incEntityIds = getEntityIdListFromEntityTagList(incEntityTags);
        // Get Entity IDs for Tags to exclude.
        Collection<EntityTag> excEntityTags = getEntityTagsForTags(ObjectType.DC, excTags);
        Collection<Long> excEntityIds = getEntityIdListFromEntityTagList(excEntityTags);
        // Create HQL.
        StringBuilder hql = new StringBuilder();
        hql.append("SELECT distinct t, count(et) ");
        hql.append("FROM Tag t ");
        hql.append("LEFT JOIN t.entityTags et ");
        hql.append("WHERE t.status != :trash ");
        hql.append("AND et.status != :trash ");
        if ((incEntityIds != null) && (!incEntityIds.isEmpty())) {
            hql.append("AND et.entityReference.entityId IN (:incEntityIds) ");
        }
        if ((excEntityIds != null) && (!excEntityIds.isEmpty())) {
            hql.append("AND et.entityReference.entityId NOT IN (:excEntityIds) ");
        }
        hql.append("GROUP BY t.tag, t.uid ");
        hql.append("ORDER BY lower(t.tag)");
        // Create Query.
        Query query = entityManager.createQuery(hql.toString());
        query.setParameter("trash", AMEEStatus.TRASH);
        if ((incEntityIds != null) && (!incEntityIds.isEmpty())) {
            query.setParameter("incEntityIds", incEntityIds);
        }
        if ((excEntityIds != null) && (!excEntityIds.isEmpty())) {
            query.setParameter("excEntityIds", excEntityIds);
        }
        query.setHint("org.hibernate.timeout", 5);
        // Execute Query, collate Tags and set count value.
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

    private List<Long> getEntityIdListFromEntityTagList(Collection<EntityTag> incEntityTags) {
        return (List<Long>) CollectionUtils.collect(incEntityTags, new Transformer() {
            public Object transform(Object entityTag) {
                return ((EntityTag) entityTag).getEntityReference().getEntityId();
            }
        });
    }

    @SuppressWarnings(value = "unchecked")
    public List<EntityTag> getEntityTags(IAMEEEntityReference entity) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(EntityTag.class);
        criteria.add(Restrictions.eq("entityReference.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entityReference.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.ne("t.status", AMEEStatus.TRASH));
        criteria.createAlias("tag", "t");
        criteria.setFetchMode("tag", FetchMode.JOIN);
        criteria.setTimeout(5);
        return criteria.list();
    }

    public EntityTag getEntityTag(IAMEEEntityReference entity, String tag) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(EntityTag.class);
        criteria.add(Restrictions.eq("entityReference.entityUid", entity.getEntityUid()));
        criteria.add(Restrictions.eq("entityReference.entityType", entity.getObjectType().getName()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.ne("t.status", AMEEStatus.TRASH));
        criteria.add(Restrictions.ilike("t.tag", tag, MatchMode.EXACT));
        criteria.createAlias("tag", "t");
        criteria.setTimeout(5);
        return (EntityTag) criteria.uniqueResult();
    }

    @SuppressWarnings(value = "unchecked")
    public List<EntityTag> getEntityTagsForEntities(ObjectType objectType, Collection<IAMEEEntityReference> entities) {
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
        criteria.add(Restrictions.ne("t.status", AMEEStatus.TRASH));
        criteria.createAlias("tag", "t");
        criteria.setFetchMode("tag", FetchMode.JOIN);
        criteria.setTimeout(5);
        return criteria.list();
    }

    @SuppressWarnings(value = "unchecked")
    public List<EntityTag> getEntityTagsForTags(ObjectType objectType, Collection<String> tags) {
        if ((tags != null) && !tags.isEmpty()) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(EntityTag.class);
            criteria.add(Restrictions.eq("entityReference.entityType", objectType.getName()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.add(Restrictions.ne("t.status", AMEEStatus.TRASH));
            criteria.add(Restrictions.in("t.tag", tags));
            criteria.createAlias("tag", "t");
            criteria.setFetchMode("tag", FetchMode.JOIN);
            criteria.setTimeout(5);
            return criteria.list();
        } else {
            // Return an empty list if the tags list is empty.
            return new ArrayList<EntityTag>();
        }
    }

    public Long getTagCount() {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setProjection(Projections.rowCount());
        criteria.setTimeout(5);
        return (Long) criteria.uniqueResult();
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