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
package com.amee.service.item;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.BaseItem;
import com.amee.domain.item.BaseItemValue;
import com.amee.domain.item.data.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DataItemServiceDAOImpl extends ItemServiceDAOImpl implements DataItemServiceDAO {

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public Class getEntityClass() {
        return DataItem.class;
    }

    // DataItems.

    /**
     * Return a count of non-trashed DataItems for the given IDataCategoryReference.
     *
     * @param dataCategory to count DataItems for
     * @return count of DataItems
     */
    public long getDataItemCount(IDataCategoryReference dataCategory) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItem.class);
        criteria.add(Restrictions.eq("dataCategory.id", dataCategory.getEntityId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataItem> getDataItems(IDataCategoryReference dataCategory) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItem.class);
        criteria.add(Restrictions.eq("dataCategory.id", dataCategory.getEntityId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataItem> getDataItems(Set<Long> dataItemIds) {
        dataItemIds.add(0L);
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItem.class);
        criteria.add(Restrictions.in("id", dataItemIds));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public DataItem getDataItemByPath(IDataCategoryReference parent, String path) {
        DataItem dataItem = null;
        if ((parent != null) && !StringUtils.isBlank(path)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataItem.class);
            criteria.add(Restrictions.eq("dataCategory.id", parent.getEntityId()));
            criteria.add(Restrictions.eq("path", path));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            List<DataItem> items = criteria.list();
            if (items.size() == 1) {
                dataItem = items.get(0);
            } else {
                log.debug("getDataItemByPath() NOT found: " + path);
            }
        }
        return dataItem;
    }

    /**
     * Returns the most recent modified timestamp of DataItems for the supplied DataCategory.
     *
     * @param dataCategory to get modified timestamp for
     * @return most recent modified timestamp
     */
    public Date getDataItemsModified(DataCategory dataCategory) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItem.class);
        criteria.add(Restrictions.eq("dataCategory.id", dataCategory.getEntityId()));
        return (Date) criteria.setProjection(Projections.max("modified")).uniqueResult();
    }

    /**
     * Returns true if the path of the supplied DataItem is unique amongst peers.
     *
     * @param dataItem to check for uniqueness
     * @return true if the DataCategory has a unique path amongst peers
     */
    @Override
    public boolean isDataItemUniqueByPath(DataItem dataItem) {
        if ((dataItem != null) && (dataItem.getDataCategory() != null)) {
            Session session = (Session) entityManager.getDelegate();
            Criteria criteria = session.createCriteria(DataItem.class);
            if (entityManager.contains(dataItem)) {
                criteria.add(Restrictions.ne("uid", dataItem.getUid()));
            }
            criteria.add(Restrictions.eq("path", dataItem.getPath()));
            criteria.add(Restrictions.eq("dataCategory.id", dataItem.getDataCategory().getId()));
            criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
            criteria.setFlushMode(FlushMode.MANUAL);
            return criteria.list().isEmpty();
        } else {
            throw new RuntimeException("DataItem was null or it doesn't have a parent DataCategory.");
        }
    }

    /**
     * Returns the DataItem matching the specified UID.
     *
     * @param uid for the requested DataItem
     * @return the matching DataItem or null if not found
     */
    @Override
    public DataItem getItemByUid(String uid) {
        return (DataItem) super.getItemByUid(uid);
    }

    @Override
    public void persist(DataItem dataItem) {
        entityManager.persist(dataItem);
    }

    // ItemValues.

    @Override
    public Set<BaseItemValue> getAllItemValues(BaseItem item) {
        if (!DataItem.class.isAssignableFrom(item.getClass())) throw new IllegalStateException();
        return getDataItemValues((DataItem) item);
    }

    @Override
    public Set<BaseItemValue> getDataItemValues(DataItem dataItem) {
        Set<BaseItemValue> rawItemValues = new HashSet<BaseItemValue>();
        rawItemValues.addAll(getDataItemNumberValues(dataItem));
        rawItemValues.addAll(getDataItemNumberValueHistories(dataItem));
        rawItemValues.addAll(getDataItemTextValues(dataItem));
        rawItemValues.addAll(getDataItemTextValueHistories(dataItem));
        return rawItemValues;
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param dataItem
     * @return
     */
    @SuppressWarnings(value = "unchecked")
    private List<DataItemNumberValueHistory> getDataItemNumberValueHistories(DataItem dataItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItemNumberValueHistory.class);
        criteria.add(Restrictions.eq("dataItem.id", dataItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param dataItem
     * @return
     */
    @SuppressWarnings(value = "unchecked")
    private List<DataItemTextValueHistory> getDataItemTextValueHistories(DataItem dataItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItemTextValueHistory.class);
        criteria.add(Restrictions.eq("dataItem.id", dataItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param dataItem
     * @return
     */
    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataItemNumberValue> getDataItemNumberValues(DataItem dataItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItemNumberValue.class);
        criteria.add(Restrictions.eq("dataItem.id", dataItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    /**
     * TODO: Would caching here be useful?
     *
     * @param dataItem
     * @return
     */
    @Override
    @SuppressWarnings(value = "unchecked")
    public List<DataItemTextValue> getDataItemTextValues(DataItem dataItem) {
        Session session = (Session) entityManager.getDelegate();
        Criteria criteria = session.createCriteria(DataItemTextValue.class);
        criteria.add(Restrictions.eq("dataItem.id", dataItem.getId()));
        criteria.add(Restrictions.ne("status", AMEEStatus.TRASH));
        return criteria.list();
    }

    @Override
    public Set<BaseItemValue> getItemValuesForItems(Collection<BaseItem> items) {
        Set<BaseItemValue> itemValues = new HashSet<BaseItemValue>();
        itemValues.addAll(getItemValuesForItems(items, DataItemNumberValue.class));
        itemValues.addAll(getItemValuesForItems(items, DataItemNumberValueHistory.class));
        itemValues.addAll(getItemValuesForItems(items, DataItemTextValue.class));
        itemValues.addAll(getItemValuesForItems(items, DataItemTextValueHistory.class));
        return itemValues;
    }
}