/**
 * This file is part of AMEE.
 *
 * AMEE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * AMEE is free software and is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by http://www.dgen.net.
 * Website http://www.amee.cc
 */
package com.amee.service.data;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.AMEEStatus;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemValue;
import com.amee.domain.environment.Environment;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Repository
public class DataServiceDAO implements Serializable {

    private final Log log = LogFactory.getLog(getClass());

    private static final String CACHE_REGION = "query.dataService";

    @PersistenceContext
    private EntityManager entityManager;

    // DataCategories

    @SuppressWarnings(value = "unchecked")
    protected DataCategory getDataCategoryByUid(String uid, boolean includeTrash) {
        DataCategory dataCategory = null;
        if (!StringUtils.isBlank(uid)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataCategory.class);
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            if (!includeTrash) {
                criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            }
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<DataCategory> dataCategories = criteria.list();
            if (dataCategories.size() == 0) {
                log.debug("getDataCategoryByUid() NOT found: " + uid);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("getDataCategoryByUid() found: " + uid);
                }
                dataCategory = dataCategories.get(0);
            }
        }
        return dataCategory;
    }

    @SuppressWarnings(value = "unchecked")
    protected DataCategory getDataCategoryByWikiName(Environment environment, String wikiName, boolean includeTrash) {
        DataCategory dataCategory = null;
        if (!StringUtils.isBlank(wikiName)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataCategory.class);
            criteria.add(Restrictions.eq("environment.id", environment.getId()));
            criteria.add(Restrictions.eq("wikiName", wikiName));
            if (!includeTrash) {
                criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            }
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<DataCategory> dataCategories = criteria.list();
            if (dataCategories.size() == 0) {
                log.debug("getDataCategoryByWikiName() NOT found: " + wikiName);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("getDataCategoryByWikiName() found: " + wikiName);
                }
                dataCategory = dataCategories.get(0);
            }
        }
        return dataCategory;
    }

    @SuppressWarnings(value = "unchecked")
    protected ResultsWrapper<DataCategory> getDataCategories(Environment environment) {
        return getDataCategories(environment, 0, 0);
    }

    @SuppressWarnings(value = "unchecked")
    protected ResultsWrapper<DataCategory> getDataCategories(Environment environment, int resultStart, int resultLimit) {
        // Create Query, apply start and limit if relevant.
        Query query = entityManager.createQuery(
                "FROM DataCategory " +
                        "WHERE environment.id = :environmentId " +
                        "AND status != :trash");
        query.setParameter("environmentId", environment.getId());
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

    @SuppressWarnings(value = "unchecked")
    protected List<DataCategory> getDataCategories(Environment environment, Set<Long> dataCategoryIds) {
        // Don't fail with an empty Set.
        if (dataCategoryIds.isEmpty()) {
            dataCategoryIds.add(0L);
        }
        return (List<DataCategory>) entityManager.createQuery(
                "FROM DataCategory " +
                        "WHERE environment.id = :environmentId " +
                        "AND status != :trash " +
                        "AND id IN (:dataCategoryIds)")
                .setParameter("environmentId", environment.getId())
                .setParameter("trash", AMEEStatus.TRASH)
                .setParameter("dataCategoryIds", dataCategoryIds)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
    }

    @SuppressWarnings(value = "unchecked")
    public List<DataCategory> getDataCategoriesModifiedWithin(
            Environment environment,
            Date modifiedSince,
            Date modifiedUntil) {
        return (List<DataCategory>) entityManager.createQuery(
                "FROM DataCategory " +
                        "WHERE environment.id = :environmentId " +
                        "AND modified >= :modifiedSince " +
                        "AND modified < :modifiedUntil")
                .setParameter("environmentId", environment.getId())
                .setParameter("modifiedSince", modifiedSince)
                .setParameter("modifiedUntil", modifiedUntil)
                .getResultList();
    }

    protected void persist(DataCategory dc) {
        entityManager.persist(dc);
    }

    @SuppressWarnings(value = "unchecked")
    protected void remove(DataCategory dataCategory) {
        log.debug("remove: " + dataCategory.toString());
        // trash this DataCategory
        dataCategory.setStatus(AMEEStatus.TRASH);
    }

    // ItemValues

    @SuppressWarnings(value = "unchecked")
    protected ItemValue getItemValueByUid(String uid) {
        ItemValue itemValue = null;
        if (!StringUtils.isBlank(uid)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(ItemValue.class);
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<ItemValue> itemValues = criteria.list();
            if (itemValues.size() == 1) {
                if (log.isTraceEnabled()) {
                    log.trace("getItemValueByUid() found: " + uid);
                }
                itemValue = itemValues.get(0);
            } else {
                log.debug("getItemValueByUid() NOT found: " + uid);
            }
        }
        return itemValue;
    }

    // DataItems

    /**
     * Returns the DatItem matching the specified UID.
     *
     * @param uid for the requested DataItem
     * @return the matching DataItem or null if not found
     */
    @SuppressWarnings(value = "unchecked")
    protected DataItem getDataItemByUid(String uid) {
        DataItem dataItem = null;
        if (!StringUtils.isBlank(uid)) {
            // See http://www.hibernate.org/117.html#A12 for notes on DISTINCT_ROOT_ENTITY.
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataItem.class);
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            criteria.add(Restrictions.naturalId().set("uid", uid.toUpperCase()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFetchMode("itemValues", FetchMode.JOIN);
            criteria.setCacheable(true);
            criteria.setCacheRegion(CACHE_REGION);
            List<DataItem> dataItems = criteria.list();
            if (dataItems.size() == 1) {
                if (log.isTraceEnabled()) {
                    log.trace("getDataItemByUid() found: " + uid);
                }
                dataItem = dataItems.get(0);
            } else {
                log.debug("getDataItemByUid() NOT found: " + uid);
            }
        }
        return dataItem;
    }

    @SuppressWarnings(value = "unchecked")
    protected DataItem getDataItemByPath(Environment environment, String path) {
        DataItem dataItem = null;
        if ((environment != null) && !StringUtils.isBlank(path)) {
            List<DataItem> dataItems = entityManager.createQuery(
                    "SELECT DISTINCT di " +
                            "FROM DataItem di " +
                            "LEFT JOIN FETCH di.itemValues " +
                            "WHERE di.path = :path " +
                            "AND di.environment.id = :environmentId " +
                            "AND di.status != :trash")
                    .setParameter("path", path)
                    .setParameter("environmentId", environment.getId())
                    .setParameter("trash", AMEEStatus.TRASH)
                    .setHint("org.hibernate.cacheable", true)
                    .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                    .getResultList();
            if (dataItems.size() == 1) {
                if (log.isTraceEnabled()) {
                    log.trace("getDataItemByPath() found: " + path);
                }
                dataItem = dataItems.get(0);
            } else {
                log.debug("getDataItemByPath() NOT found: " + path);
            }
        }
        return dataItem;
    }

    @SuppressWarnings(value = "unchecked")
    protected List<DataItem> getDataItems(DataCategory dataCategory) {
        log.debug("getDataItems() Start: " + dataCategory.toString());
        List<DataItem> dataItems = entityManager.createQuery(
                "SELECT DISTINCT di " +
                        "FROM DataItem di " +
                        "LEFT JOIN FETCH di.itemValues " +
                        "WHERE di.itemDefinition.id = :itemDefinitionId " +
                        "AND di.dataCategory.id = :dataCategoryId " +
                        "AND di.status != :trash")
                .setParameter("itemDefinitionId", dataCategory.getItemDefinition().getId())
                .setParameter("dataCategoryId", dataCategory.getId())
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        log.debug("getDataItems() Done: " + dataCategory.toString() + " (" + dataItems.size() + ")");
        return dataItems;
    }

    protected List<DataItem> getDataItems(Environment environment, Set<Long> dataItemIds) {
        return getDataItems(environment, dataItemIds, false);
    }

    @SuppressWarnings(value = "unchecked")
    protected List<DataItem> getDataItems(Environment environment, Set<Long> dataItemIds, boolean values) {
        // Don't fail with an empty Set.
        if (dataItemIds.isEmpty()) {
            dataItemIds.add(0L);
        }
        StringBuilder hql = new StringBuilder();
        hql.append("SELECT DISTINCT di ");
        hql.append("FROM DataItem di ");
        if (values) {
            hql.append("LEFT JOIN FETCH di.itemValues ");
        }
        hql.append("WHERE di.environment.id = :environmentId ");
        hql.append("AND di.status != :trash ");
        hql.append("AND di.id IN (:dataItemIds)");
        return (List<DataItem>) entityManager.createQuery(hql.toString())
                .setParameter("environmentId", environment.getId())
                .setParameter("trash", AMEEStatus.TRASH)
                .setParameter("dataItemIds", dataItemIds)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
    }

    protected void persist(DataItem dataItem) {
        entityManager.persist(dataItem);
    }

    protected void remove(DataItem dataItem) {
        dataItem.setStatus(AMEEStatus.TRASH);
    }

    protected void remove(ItemValue dataItemValue) {
        dataItemValue.setStatus(AMEEStatus.TRASH);
    }
}