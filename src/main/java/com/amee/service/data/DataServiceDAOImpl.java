package com.amee.service.data;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.*;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataCategoryReference;
import com.amee.domain.data.ItemDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.*;

@Repository
public class DataServiceDAOImpl implements DataServiceDAO {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CACHE_REGION = "query.dataService";

    @PersistenceContext
    private EntityManager entityManager;

    // DataCategories

    @Override
    @SuppressWarnings(value = "unchecked")
    public DataCategory getRootDataCategory() {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataCategory.class);
        criteria.add(Restrictions.eq("path", ""));
        criteria.add(Restrictions.isNull("dataCategory"));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        List<DataCategory> dataCategories = criteria.list();
        if (dataCategories.size() == 0) {
            throw new RuntimeException("Root Data Category not found.");
        } else {
            return dataCategories.get(0);
        }
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public DataCategory getDataCategoryByPath(IDataCategoryReference parent, String path) {
        DataCategory dataCategory = null;
        if ((parent != null) && !StringUtils.isBlank(path)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataCategory.class);
            criteria.add(Restrictions.eq("dataCategory.id", parent.getEntityId()));
            criteria.add(Restrictions.ilike("path", path, MatchMode.EXACT));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<DataCategory> dataCategories = criteria.list();
            if (dataCategories.size() == 0) {
                log.debug("getDataCategoryByPath() DataCategory not found ('{}').", parent.getFullPath() + "/" + path);
            } else if (dataCategories.size() == 1) {
                dataCategory = dataCategories.get(0);
            } else {
                log.warn("getDataCategoryByPath() More than one DataCategory found ('" + parent.getFullPath() + "/" + path + "').");
            }
        }
        return dataCategory;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public DataCategory getDataCategoryByUidWithAnyStatus(String uid) {
        DataCategory dataCategory = null;
        if (!StringUtils.isBlank(uid)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataCategory.class);
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<DataCategory> dataCategories = criteria.list();
            if (dataCategories.size() == 0) {
                log.debug("getDataCategoryByUid() NOT found: {}", uid);
            } else {
                dataCategory = dataCategories.get(0);
            }
        }
        return dataCategory;
    }

    @Override
    public DataCategory getDataCategoryByUidWithActiveStatus(String uid) {
        return getDataCategoryByUidWithStatus(uid, AMEEStatus.ACTIVE);
    }

    @Override
    public DataCategory getDataCategoryByUidWithStatus(String uid, AMEEStatus status) {
        return getDataCategoryWithStatus(getDataCategoryByUidWithAnyStatus(uid), status);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public DataCategory getDataCategoryByWikiName(String wikiName, AMEEStatus status) {
        DataCategory dataCategory = null;
        if (!StringUtils.isBlank(wikiName) && (status != null)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataCategory.class);
            criteria.add(Restrictions.eq("wikiName", wikiName));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            criteria.setFlushMode(FlushMode.MANUAL);
            List<DataCategory> dataCategories = criteria.list();
            // Remove Data Categories that don't match the requested status.
            Iterator<DataCategory> i = dataCategories.iterator();
            while (i.hasNext()) {
                DataCategory dc = i.next();
                boolean trashed = dc.isTrash();
                if (status.equals(AMEEStatus.TRASH) && !trashed) {
                    // Requested status IS TRASH but Data Category is not TRASHed, so remove it.
                    i.remove();
                } else if (!status.equals(AMEEStatus.TRASH) && trashed) {
                    // Requested status is NOT TRASH but DataCategory is TRASHed, so remove it.
                    i.remove();
                } else if (!dc.getStatus().equals(status)) {
                    // Requested status does not match DataCategory status, so remove it.
                    i.remove();
                }
            }
            // Do we have any Data Categories?
            if (dataCategories.size() == 0) {
                log.debug("getDataCategoryByWikiName() NOT found: {}", wikiName);
            } else {
                // Special handling required when more than one DataCategory is found.
                if (dataCategories.size() > 1) {
                    log.debug("getDataCategoryByWikiName() More than one DataCategory found: {}", wikiName);
                    // Sort the DataCategories by modification date (descending).
                    Collections.sort(dataCategories,
                            Collections.reverseOrder(
                                    new Comparator<DataCategory>() {
                                        @Override
                                        public int compare(DataCategory o1, DataCategory o2) {
                                            return (o1.getModified().compareTo(o2.getModified()));
                                        }
                                    }));
                }
                // Return the only (or most recently modified) DataCategory.
                dataCategory = dataCategories.get(0);
            }
        }
        return dataCategory;
    }

    /**
     * Return the supplied DataCategory if the status matches.
     *
     * @param dataCategory to check status against
     * @param status only return DataCategories with the given status. If null, return any status.
     * @return the supplied DataCategory if it matches the status, otherwise null
     */
    @Override
    public DataCategory getDataCategoryWithStatus(DataCategory dataCategory, AMEEStatus status) {
        if (dataCategory != null) {

            // Was a specific status requested?
            if (status != null) {

                // Specific status requested.
                boolean trashed = dataCategory.isTrash();
                if (status.equals(AMEEStatus.TRASH) && trashed) {

                    // TRASHed status requested and DataCategory IS trashed.
                    return dataCategory;
                } else if (!trashed) {

                    // ACTIVE or DEPRECATED status requested and DataCategory is NOT trashed.
                    return dataCategory;
                } else {

                    // Not found.
                    return null;
                }
            } else {

                // Allow any status.
                return dataCategory;
            }
        } else {
            return null;
        }
    }

    /**
     * Get full DataCategory entity based on the supplied IDataCategoryReference.
     *
     * @param dataCategory IDataCategoryReference to fetch a DataCategory for
     * @return the DataCategory matching the IDataCategoryReference
     */
    @Override
    public DataCategory getDataCategory(IDataCategoryReference dataCategory) {
        DataCategory dc = getDataCategoryWithStatus(entityManager.find(DataCategory.class, dataCategory.getEntityId()), AMEEStatus.ACTIVE);
        if (dc == null) {
            throw new IllegalStateException("DataCategory should not be null.");
        }
        return dc;
    }

    @Override
    public ResultsWrapper<DataCategory> getDataCategories() {
        return getDataCategories(0, 0);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public ResultsWrapper<DataCategory> getDataCategories(int resultStart, int resultLimit) {
        // Create Query, apply start and limit if relevant.
        Query query = entityManager.createQuery(
                "FROM DataCategory " +
                        "WHERE status != :trash " +
                        "ORDER BY LOWER(wikiName) ASC");
        query.setParameter("trash", AMEEStatus.TRASH);
        query.setHint("org.hibernate.cacheable", true);
        query.setHint("org.hibernate.cacheRegion", CACHE_REGION);
        if (resultStart > 0) {
            query.setFirstResult(resultStart);
        }
        if (resultLimit > 0) {
            query.setMaxResults(resultLimit + 1);
        }
        // Get the results.
        List<DataCategory> dataCategories = (List<DataCategory>) query.getResultList();
        // Did we limit the results?
        if (resultLimit > 0) {
            // Results were limited, work out correct results and truncation state.
            return new ResultsWrapper<DataCategory>(
                    dataCategories.size() > resultLimit ? dataCategories.subList(0, resultLimit) : dataCategories,
                    dataCategories.size() > resultLimit);

        } else {
            // Results were not limited, no truncation.
            return new ResultsWrapper(dataCategories, false);
        }
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataCategory> getDataCategories(Set<Long> dataCategoryIds) {
        // Don't fail with an empty Set.
        if (dataCategoryIds.isEmpty()) {
            dataCategoryIds.add(0L);
        }
        return (List<DataCategory>) entityManager.createQuery(
                "FROM DataCategory " +
                        "WHERE status != :trash " +
                        "AND id IN (:dataCategoryIds)")
                .setParameter("trash", AMEEStatus.TRASH)
                .setParameter("dataCategoryIds", dataCategoryIds)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataCategory> getDataCategoriesModifiedWithin(
            Date modifiedSince,
            Date modifiedUntil) {
        return (List<DataCategory>) entityManager.createQuery(
                "FROM DataCategory " +
                        "WHERE modified >= :modifiedSince " +
                        "AND modified < :modifiedUntil")
                .setParameter("modifiedSince", modifiedSince)
                .setParameter("modifiedUntil", modifiedUntil)
                .getResultList();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataCategory> getDataCategoriesForDataItemsModifiedWithin(
            Date modifiedSince,
            Date modifiedUntil) {
        return (List<DataCategory>) entityManager.createQuery(
                "SELECT DISTINCT di.dataCategory " +
                        "FROM DataItem di " +
                        "WHERE di.modified >= :modifiedSince " +
                        "AND di.modified < :modifiedUntil")
                .setParameter("modifiedSince", modifiedSince)
                .setParameter("modifiedUntil", modifiedUntil)
                .getResultList();
    }

    /**
     * Returns a List of IDataCategoryReferences whose parent matches the IDataCategoryReference supplied. Will
     * exclude all Ecoinvent categories.
     *
     * @param dataCategoryReference
     * @return
     */
    @Override
    @SuppressWarnings(value = "unchecked")
    public Map<String, IDataCategoryReference> getDataCategories(IDataCategoryReference dataCategoryReference) {
        Map<String, IDataCategoryReference> dataCategoriesReferences =
                new TreeMap<String, IDataCategoryReference>(String.CASE_INSENSITIVE_ORDER);
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataCategory.class);
        criteria.add(Restrictions.eq("dataCategory.id", dataCategoryReference.getEntityId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        criteria.addOrder(Order.asc("path").ignoreCase());
        criteria.setCacheable(true);
        criteria.setCacheRegion(CACHE_REGION);
        criteria.setFlushMode(FlushMode.MANUAL);
        List<DataCategory> dataCategories = criteria.list();
        for (DataCategory dc : dataCategories) {
            if (!dc.isTrash()) {
                dataCategoriesReferences.put(dc.getPath(), new DataCategoryReference(dc));
            }
        }
        return dataCategoriesReferences;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Set<AMEEEntityReference> getDataCategoryReferences(ItemDefinition itemDefinition) {
        Set<AMEEEntityReference> dataCategoryReferences = new HashSet<AMEEEntityReference>();
        if (itemDefinition != null) {
            List<Object[]> rows = entityManager.createQuery(
                    "SELECT id, uid " +
                            "FROM DataCategory " +
                            "WHERE itemDefinition.id = :itemDefinitionId " +
                            "AND status != :trash")
                    .setParameter("itemDefinitionId", itemDefinition.getId())
                    .setParameter("trash", AMEEStatus.TRASH)
                    .getResultList();
            for (Object[] row : rows) {
                dataCategoryReferences.add(new AMEEEntityReference(ObjectType.DC, (Long) row[0], (String) row[1]));
            }
        }
        return dataCategoryReferences;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Set<Long> getParentDataCategoryIds(Set<Long> dataCategoryIds) {
        Set<Long> parentDataCategoryIds = new HashSet<Long>();
        if ((dataCategoryIds != null) && !dataCategoryIds.isEmpty()) {
            parentDataCategoryIds.addAll(
                    (List<Long>) entityManager.createQuery(
                            "SELECT dataCategory.id " +
                                    "FROM DataCategory " +
                                    "WHERE id in (:dataCategoryIds) " +
                                    "AND status != :trash")
                            .setParameter("dataCategoryIds", dataCategoryIds)
                            .setParameter("trash", AMEEStatus.TRASH)
                            .getResultList());
            parentDataCategoryIds.remove(null);
        }
        return parentDataCategoryIds;
    }

    /**
     * Returns true if the path of the supplied DataCategory is unique amongst peers.
     *
     * @param dataCategory to check for uniqueness
     * @return true if the DataCategory has a unique path amongst peers
     */
    @Override
    public boolean isDataCategoryUniqueByPath(DataCategory dataCategory) {
        if ((dataCategory != null) && (dataCategory.getDataCategory() != null)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataCategory.class);
            if (entityManager.contains(dataCategory)) {
                criteria.add(Restrictions.ne("uid", dataCategory.getUid()));
            }
            criteria.add(Restrictions.eq("path", dataCategory.getPath()));
            criteria.add(Restrictions.eq("dataCategory.id", dataCategory.getDataCategory().getId()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFlushMode(FlushMode.MANUAL);
            return criteria.list().isEmpty();
        } else {
            throw new RuntimeException("DataCategory was null or it doesn't have a parent.");
        }
    }

    /**
     * Returns true if the wikiName of the supplied DataCategory is unique.
     *
     * @param dataCategory to check for uniqueness
     * @return true if the DataCategory has a unique wikiName
     */
    @Override
    public boolean isDataCategoryUniqueByWikiName(DataCategory dataCategory) {
        if (dataCategory != null) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataCategory.class);
            if (entityManager.contains(dataCategory)) {
                criteria.add(Restrictions.ne("uid", dataCategory.getUid()));
            }
            criteria.add(Restrictions.eq("wikiName", dataCategory.getWikiName()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFlushMode(FlushMode.MANUAL);
            return criteria.list().isEmpty();
        } else {
            throw new RuntimeException("DataCategory was null.");
        }
    }

    @Override
    public void persist(DataCategory dc) {
        entityManager.persist(dc);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void remove(DataCategory dataCategory) {
        log.debug("remove() {}", dataCategory.toString());
        // trash this DataCategory
        dataCategory.setStatus(AMEEStatus.TRASH);
    }

    @Override
    public void invalidate(DataCategory dataCategory) {
        log.debug("invalidate() {}", dataCategory.toString());
        ((Session) entityManager.getDelegate()).getSessionFactory().getCache().evictEntity(DataCategory.class, dataCategory.getId());
    }

    //  API Versions

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<APIVersion> getAPIVersions() {
        return entityManager.createQuery(
                "FROM APIVersion av " +
                        "WHERE av.status != :trash " +
                        "ORDER BY av.version")
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
    }

    /**
     * Gets an APIVersion based on the supplied version parameter.
     *
     * @param version to fetch
     * @return APIVersion object, or null
     */
    @Override
    public APIVersion getAPIVersion(String version) {
        try {
            return (APIVersion) entityManager.createQuery(
                    "FROM APIVersion av " +
                            "WHERE av.version = :version " +
                            "AND av.status != :trash")
                    .setParameter("version", version)
                    .setParameter("trash", AMEEStatus.TRASH)
                    .setHint("org.hibernate.cacheable", true)
                    .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            return null;
        }
    }
}
