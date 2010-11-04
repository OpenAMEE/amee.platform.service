package com.amee.service.auth;

import com.amee.domain.*;
import com.amee.domain.auth.Group;
import com.amee.domain.auth.GroupPrincipal;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class GroupServiceDAO {

    private static final String CACHE_REGION = "query.groupService";

    @PersistenceContext
    private EntityManager entityManager;

    // Groups

    @SuppressWarnings(value = "unchecked")
    public Group getGroupByUid(String uid) {
        Group group = null;
        List<Group> groups = entityManager.createQuery(
                "SELECT g FROM Group g " +
                        "WHERE g.uid = :uid " +
                        "AND g.status != :trash")
                .setParameter("uid", uid)
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        if (groups.size() > 0) {
            group = groups.get(0);
        }
        return group;
    }

    @SuppressWarnings(value = "unchecked")
    public Group getGroupByName(String name) {
        Group group = null;
        List<Group> groups = entityManager.createQuery(
                "SELECT g FROM Group g " +
                        "WHERE g.name = :name " +
                        "AND g.status != :trash")
                .setParameter("name", name.trim())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        if (groups.size() > 0) {
            group = groups.get(0);
        }
        return group;
    }

    @SuppressWarnings(value = "unchecked")
    public List<Group> getGroups() {
        List<Group> groups = entityManager.createQuery(
                "SELECT g " +
                        "FROM Group g " +
                        "WHERE g.status != :trash " +
                        "ORDER BY g.name")
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        return groups;
    }

    public List<Group> getGroups(Pager pager) {

        Query query;

        String pagerSetClause = getPagerSetClause("g", pager);

        // first count all objects
        query = entityManager.createQuery(
                "SELECT count(g) " +
                        "FROM Group g " +
                        "WHERE 1 = 1 " + pagerSetClause + " " +
                        "AND g.status != :trash ")
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION);
        if (!"".equals(pagerSetClause)) {
            query.setParameter("pagerSet", pager.getPagerSet());
        }
        // tell pager how many objects there are and give it a chance to select the requested page again
        long count = (Long) query.getSingleResult();
        pager.setItems(count);
        pager.goRequestedPage();
        // now get the objects for the current page
        query = entityManager.createQuery(
                "SELECT g " +
                        "FROM Group g " +
                        "WHERE 1 = 1 " + pagerSetClause + " " +
                        "AND g.status != :trash " +
                        "ORDER BY g.name")
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .setMaxResults(pager.getItemsPerPage())
                .setFirstResult((int) pager.getStart());
        if (!"".equals(pagerSetClause)) {
            query.setParameter("pagerSet", pager.getPagerSet());
        }
        List<Group> groups = query.getResultList();
        // update the pager
        pager.setItemsFound(groups.size());
        // all done, return results
        return groups;
    }

    private String getPagerSetClause(String alias, Pager pager) {

        StringBuilder ret = new StringBuilder();
        if (pager.isPagerSetApplicable()) {
            if (pager.getPagerSetType().equals(PagerSetType.INCLUDE)) {
                ret.append(" AND ");
                ret.append(alias);
                ret.append(" IN (:pagerSet) ");
            } else {
                ret.append(" AND ");
                ret.append(alias);
                ret.append(" NOT IN (:pagerSet) ");
            }
        }
        return ret.toString();
    }

    public void save(Group group) {
        entityManager.persist(group);
    }

    public void remove(Group group) {
        group.setStatus(AMEEStatus.TRASH);
    }

    // GroupPrincipals

    @SuppressWarnings(value = "unchecked")
    public GroupPrincipal getGroupPrincipalByUid(String uid) {
        GroupPrincipal groupPrincipal = null;
        if ((uid != null)) {
            List<GroupPrincipal> groupPrincipals = entityManager.createQuery(
                    "SELECT gp FROM GroupPrincipal gp " +
                            "WHERE gp.uid = :uid " +
                            "AND gp.status != :trash")
                    .setParameter("uid", uid)
                    .setParameter("trash", AMEEStatus.TRASH)
                    .setHint("org.hibernate.cacheable", true)
                    .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                    .getResultList();
            if (groupPrincipals.size() > 0) {
                groupPrincipal = groupPrincipals.get(0);
            }
        }
        return groupPrincipal;
    }

    @SuppressWarnings(value = "unchecked")
    public GroupPrincipal getGroupPrincipal(Group group, IAMEEEntityReference entity) {
        GroupPrincipal groupPrincipal = null;
        if ((group != null) && (entity != null)) {
            List<GroupPrincipal> groupPrincipals = entityManager.createQuery(
                    "SELECT gp FROM GroupPrincipal gp " +
                            "WHERE gp.group.id = :groupId " +
                            "AND gp.principalReference.entityUid = :entityUid " +
                            "AND gp.principalReference.entityType = :entityType " +
                            "AND gp.status != :trash")
                    .setParameter("groupId", group.getId())
                    .setParameter("entityUid", entity.getEntityUid())
                    .setParameter("entityType", entity.getObjectType().getName())
                    .setParameter("trash", AMEEStatus.TRASH)
                    .setHint("org.hibernate.cacheable", true)
                    .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                    .getResultList();
            if (groupPrincipals.size() > 0) {
                groupPrincipal = groupPrincipals.get(0);
            }
        }
        return groupPrincipal;
    }

    @SuppressWarnings(value = "unchecked")
    public List<GroupPrincipal> getGroupPrincipals(Group group, Pager pager) {
        // first count all objects
        long count = (Long) entityManager.createQuery(
                "SELECT count(gp) " +
                        "FROM GroupPrincipal gp " +
                        "WHERE gp.group.id = :groupId " +
                        "AND gp.status != :trash")
                .setParameter("groupId", group.getId())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getSingleResult();
        // tell pager how many objects there are and give it a chance to select the requested page again
        pager.setItems(count);
        pager.goRequestedPage();
        // now get the objects for the current page
        List<GroupPrincipal> groupPrincipals = entityManager.createQuery(
                "SELECT gp " +
                        "FROM GroupPrincipal gp " +
                        "WHERE gp.group.id = :groupId " +
                        "AND gp.status != :trash")
                .setParameter("groupId", group.getId())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .setMaxResults(pager.getItemsPerPage())
                .setFirstResult((int) pager.getStart())
                .getResultList();
        // update the pager
        pager.setItemsFound(groupPrincipals.size());
        // all done, return results
        return groupPrincipals;
    }

    @SuppressWarnings(value = "unchecked")
    public List<GroupPrincipal> getGroupPrincipalsForPrincipal(IAMEEEntityReference principal, Pager pager) {
        // first count all objects
        long count = (Long) entityManager.createQuery(
                "SELECT count(gp) " +
                        "FROM GroupPrincipal gp " +
                        "WHERE gp.principalReference.entityUid = :entityUid " +
                        "AND gp.principalReference.entityType = :entityType " +
                        "AND gp.status != :trash")
                .setParameter("entityUid", principal.getEntityUid())
                .setParameter("entityType", principal.getObjectType().getName())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getSingleResult();
        // tell pager how many objects there are and give it a chance to select the requested page again
        pager.setItems(count);
        pager.goRequestedPage();
        // now get the objects for the current page
        List<GroupPrincipal> groupPrincipals = entityManager.createQuery(
                "SELECT gp " +
                        "FROM GroupPrincipal gp " +
                        "LEFT JOIN FETCH gp.group g " +
                        "WHERE gp.principalReference.entityUid = :entityUid " +
                        "AND gp.principalReference.entityType = :entityType " +
                        "AND gp.status != :trash " +
                        "ORDER BY g.name")
                .setParameter("entityUid", principal.getEntityUid())
                .setParameter("entityType", principal.getObjectType().getName())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .setMaxResults(pager.getItemsPerPage())
                .setFirstResult((int) pager.getStart())
                .getResultList();
        // update the pager
        pager.setItemsFound(groupPrincipals.size());
        // all done, return results
        return groupPrincipals;
    }

    public Set<Group> getGroupsForPrincipal(AMEEEntity principal) {
        Set<Group> groups = new HashSet<Group>();
        for (GroupPrincipal groupPrincipal : getGroupPrincipalsForPrincipal(principal)) {
            groups.add(groupPrincipal.getGroup());
        }
        return groups;
    }

    @SuppressWarnings(value = "unchecked")
    public List<GroupPrincipal> getGroupPrincipalsForPrincipal(IAMEEEntityReference principal) {
        List<GroupPrincipal> groupPrincipals = entityManager.createQuery(
                "SELECT gp " +
                        "FROM GroupPrincipal gp " +
                        "LEFT JOIN FETCH gp.group g " +
                        "WHERE gp.principalReference.entityUid = :entityUid " +
                        "AND gp.principalReference.entityType = :entityType " +
                        "AND gp.status != :trash " +
                        "ORDER BY g.name")
                .setParameter("entityUid", principal.getEntityUid())
                .setParameter("entityType", principal.getObjectType().getName())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        // all done, return results
        return groupPrincipals;
    }

    @SuppressWarnings(value = "unchecked")
    public List<GroupPrincipal> getGroupPrincipals() {
        List<GroupPrincipal> groupPrincipals = entityManager.createQuery(
                "SELECT gp " +
                        "FROM GroupPrincipal gp " +
                        "WHERE gp.status != :trash")
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        return groupPrincipals;
    }

    public void save(GroupPrincipal groupPrincipal) {
        entityManager.persist(groupPrincipal);
    }

    public void remove(GroupPrincipal groupPrincipal) {
        groupPrincipal.setStatus(AMEEStatus.TRASH);
    }
}
