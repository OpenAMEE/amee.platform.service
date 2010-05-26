package com.amee.service.tag;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.tag.EntityTag;
import com.amee.domain.tag.Tag;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TagServiceDAO implements Serializable {

    @PersistenceContext
    private EntityManager entityManager;

    protected Tag getTag(String tag) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.add(Restrictions.ilike("tag", tag, MatchMode.EXACT));
        criteria.setTimeout(1);
        return (Tag) criteria.uniqueResult();
    }

    @SuppressWarnings(value = "unchecked")
    protected List<Tag> getTags() {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setTimeout(1);
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
                        "GROUP BY t.tag " +
                        "ORDER BY t.tag");
        query.setParameter("trash", AMEEStatus.TRASH);
        query.setHint("org.hibernate.timeout", 1);
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
        criteria.setTimeout(1);
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
        criteria.setTimeout(1);
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