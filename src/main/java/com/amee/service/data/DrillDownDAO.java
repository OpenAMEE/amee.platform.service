/*
 * This file is part of AMEE.
 *
 * Copyright (c) 2007, 2008, 2009 AMEE UK LIMITED (help@amee.com).
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

import com.amee.domain.AMEEStatus;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.LocaleHolder;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.sheet.Choice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.*;

/**
 * Uses native SQL to perform a 'drill down' into DataItem values.
 * <p/>
 * See {@link DrillDownService} for a description of drill downs.
 * <p/>
 * Note: I was unable to use the JPA EntityManger for this SQL so have used
 * the native Hibernate Session instead. This seems to be due to
 * the ITEM_VALUE.VALUE column being MEDIUMTEXT. This is the error message
 * I was getting: "No Dialect mapping for JDBC type: -1". After lots of
 * searching the options seem to be: 1) create a custom hibernate SQL dialect
 * that can handle MEDIUMTEXT to String conversion, 2) change the type of the
 * VALUE column, 3) use SQLQuery.addScalar to get hibernate to understand
 * the VALUE column. I've opted for option 3 here.
 */
@Service
class DrillDownDAO implements Serializable {

    private final Log log = LogFactory.getLog(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataServiceDAO dataServiceDao;

    /**
     * Retrieves a {@link List} of {@link Choice}s containing values for a user to select. The value choices
     * are appropriate for the current level within the 'drill down' given the supplied {@link DataCategory},
     * {@link com.amee.domain.data.ItemValueDefinition) path and selections.
     *
     * @param dataCategory the {@link com.amee.domain.data.DataCategory} from which {@link com.amee.domain.data.DataItem}s will
     *                     be selected (required)
     * @param path         the path of the {@link com.amee.domain.data.ItemValueDefinition) from which to select values
     * @param selections   the current user selections for a drill down
     * @return a {@link java.util.List} of {@link com.amee.domain.sheet.Choice}s containing values for a user to select
     */
    public List<Choice> getDataItemValueChoices(
            IDataCategoryReference dc,
            String path,
            List<Choice> selections) {

        Collection<Long> dataItemIds;

        // check arguments
        if ((dc == null) ||
                (!dc.isItemDefinitionPresent()) ||
                (selections == null) ||
                (path == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // Get the Data Category.
        DataCategory dataCategory = dataServiceDao.getDataCategory(dc);

        // get choices
        List<Choice> choices = new ArrayList<Choice>();
        ItemDefinition itemDefinition = dataCategory.getItemDefinition();
        ItemValueDefinition itemValueDefinition = itemDefinition.getItemValueDefinition(path);
        if (itemValueDefinition != null) {
            // Get Data Item IDs.
            if (!selections.isEmpty()) {
                // Get Data Item IDs based on selections.
                dataItemIds = getDataItemIds(dataCategory, selections);
            } else {
                // Get Data Item IDs for top level (no selections).
                dataItemIds = getDataItemIds(dataCategory.getEntityId(), itemDefinition.getId());
            }
            // Get choices.
            if (!dataItemIds.isEmpty()) {
                for (String value : getDataItemValues(itemValueDefinition.getId(), dataItemIds)) {
                    choices.add(new Choice(value));
                }
            }
        } else {
            throw new IllegalArgumentException("ItemValueDefinition not found: " + path);
        }

        return choices;
    }

    /**
     * Retrieves a {@link List} of {@link Choice}s containing values for a user to select. The value choices
     * are appropriate for the current level within the 'drill down' given the supplied {@link DataCategory},
     * {@link com.amee.domain.data.ItemValueDefinition) path and selections.
     *
     * @param dataCategory the {@link com.amee.domain.data.DataCategory} from which {@link com.amee.domain.data.DataItem}s will
     *                     be selected (required)
     * @param selections   the current user selections for a drill down
     * @return a {@link java.util.List} of {@link com.amee.domain.sheet.Choice}s containing UIDs for a user to select
     */
    public List<Choice> getDataItemUIDChoices(IDataCategoryReference dc, List<Choice> selections) {

        // check arguments
        if ((dc == null) || (!dc.isItemDefinitionPresent())) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // Get the Data Category.
        DataCategory dataCategory = dataServiceDao.getDataCategory(dc);

        // get choices
        List<Choice> choices = new ArrayList<Choice>();
        ItemDefinition itemDefinition = dataCategory.getItemDefinition();
        Collection<Long> dataItemIds;
        if (!selections.isEmpty()) {
            // get choices based on selections
            dataItemIds = getDataItemIds(dataCategory, selections);
            if (!dataItemIds.isEmpty()) {
                for (String value : this.getDataItemUIDs(dataItemIds)) {
                    choices.add(new Choice(value));
                }
            }
        } else {
            // get choices for top level (no selections)
            for (String value : getDataItemUIDs(dataCategory.getEntityId(), itemDefinition.getId())) {
                choices.add(new Choice(value));
            }
        }

        return choices;
    }

    @SuppressWarnings(value = "unchecked")
    private Collection<String> getDataItemUIDs(Collection<Long> dataItemIds) {

        StringBuilder sql;
        SQLQuery query;

        // check arguments
        if ((dataItemIds == null) ||
                (dataItemIds.isEmpty())) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // create SQL
        sql = new StringBuilder();
        sql.append("SELECT UID ");
        sql.append("FROM ITEM ");
        sql.append("WHERE ID IN (:dataItemIds) ");
        sql.append("AND STATUS != :trash");

        // create query
        Session session = (Session) entityManager.getDelegate();
        query = session.createSQLQuery(sql.toString());
        query.addScalar("UID", Hibernate.STRING);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setParameterList("dataItemIds", dataItemIds, Hibernate.LONG);

        // execute SQL
        try {
            List<String> results = query.list();
            log.debug("getDataItemUIDs() results: " + results.size());
            return results;
        } catch (HibernateException e) {
            log.error("getDataItemUIDs() Caught HibernateException: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Collection<String> getDataItemUIDs(Long dataCategoryId, Long itemDefinitionId) {

        // check arguments
        if ((dataCategoryId == null) || (itemDefinitionId == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // create SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT UID ");
        sql.append("FROM ITEM ");
        sql.append("WHERE TYPE = 'DI' ");
        sql.append("AND STATUS != :trash ");
        sql.append("AND DATA_CATEGORY_ID = :dataCategoryId ");
        sql.append("AND ITEM_DEFINITION_ID = :itemDefinitionId");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("UID", Hibernate.STRING);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setLong("dataCategoryId", dataCategoryId);
        query.setLong("itemDefinitionId", itemDefinitionId);

        // execute SQL
        List<String> dataItemUids = query.list();
        log.debug("getDataItemUIDs() results: " + dataItemUids.size());
        return new HashSet<String>(dataItemUids);
    }

    private Collection<Long> getDataItemIds(Long dataCategoryId, Long itemDefinitionId) {

        // check arguments
        if ((dataCategoryId == null) || (itemDefinitionId == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // create SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ID ");
        sql.append("FROM ITEM ");
        sql.append("WHERE TYPE = 'DI' ");
        sql.append("AND STATUS != :trash ");
        sql.append("AND DATA_CATEGORY_ID = :dataCategoryId ");
        sql.append("AND ITEM_DEFINITION_ID = :itemDefinitionId");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("ID", Hibernate.LONG);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setLong("dataCategoryId", dataCategoryId);
        query.setLong("itemDefinitionId", itemDefinitionId);

        // execute SQL
        List<Long> dataItemIds = query.list();
        log.debug("getDataItemIds() results: " + dataItemIds.size());
        return new HashSet<Long>(dataItemIds);
    }

    @SuppressWarnings(value = "unchecked")
    private List<String> getDataItemValues(Long itemValueDefinitionId, Collection<Long> dataItemIds) {

        // check arguments
        if ((itemValueDefinitionId == null) || (dataItemIds == null) || (dataItemIds.isEmpty())) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // create SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT VALUE ");
        sql.append("FROM ITEM_VALUE ");
        sql.append("WHERE ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
        sql.append("AND ITEM_ID IN (:dataItemIds) ");
        sql.append("ORDER BY LCASE(VALUE) ASC");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("VALUE", Hibernate.STRING);

        // set parameters
        query.setLong("itemValueDefinitionId", itemValueDefinitionId);
        query.setParameterList("dataItemIds", dataItemIds, Hibernate.LONG);

        // execute SQL
        try {
            List<String> results = query.list();
            log.debug("getDataItemValues() results: " + results.size());
            return results;
        } catch (HibernateException e) {
            log.error("getDataItemValues() Caught HibernateException: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Collection<Long> getDataItemIds(IDataCategoryReference dc, List<Choice> selections) {

        // check arguments
        if ((dc == null) ||
                (!dc.isItemDefinitionPresent()) ||
                (selections == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // Get the Data Category.
        DataCategory dataCategory = dataServiceDao.getDataCategory(dc);

        // iterate over selections and fetch DataItem IDs
        Set<Long> allDataItemIds = new HashSet<Long>();
        Collection<Collection<Long>> collections = new ArrayList<Collection<Long>>();
        ItemValueDefinition itemValueDefinition;
        Collection<Long> dataItemIds;
        for (Choice selection : selections) {
            itemValueDefinition = dataCategory.getItemDefinition().getItemValueDefinition(selection.getName());
            if (itemValueDefinition != null) {
                dataItemIds = getDataItemIds(dataCategory.getEntityId(), itemValueDefinition.getId(), selection.getValue());
                collections.add(dataItemIds);
                allDataItemIds.addAll(dataItemIds);
            } else {
                throw new IllegalArgumentException("Could not locate ItemValueDefinition: " + selection.getName());
            }
        }

        // reduce all to intersection
        for (Collection<Long> c : collections) {
            allDataItemIds.retainAll(c);
        }

        return allDataItemIds;
    }

    @SuppressWarnings(value = "unchecked")
    private Collection<Long> getDataItemIds(Long dataCategoryId, Long itemValueDefinition, String value) {

        Set<Long> dataItemIds;
        if (LocaleHolder.isDefaultLocale()) {
            dataItemIds = getDataItemIdsUsingValue(dataCategoryId, itemValueDefinition, value);
        } else {
            dataItemIds = getDataItemIdsUsingLocaleNames(dataCategoryId, itemValueDefinition, value);
        }

        log.debug("getDataItemIds() results: " + dataItemIds.size());

        return dataItemIds;
    }

    @SuppressWarnings("unchecked")
    private Set<Long> getDataItemIdsUsingValue(
            Long dataCategoryId,
            Long itemValueDefinitionId,
            String value) {

        // create SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT i.ID ID ");
        sql.append("FROM ITEM i, ITEM_VALUE iv ");
        sql.append("WHERE i.ID = iv.ITEM_ID ");
        sql.append("AND i.STATUS != :trash ");
        sql.append("AND i.TYPE = 'DI' ");
        sql.append("AND i.DATA_CATEGORY_ID = :dataCategoryId ");
        sql.append("AND iv.ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
        sql.append("AND iv.VALUE = :value");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("ID", Hibernate.LONG);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setLong("dataCategoryId", dataCategoryId);
        query.setLong("itemValueDefinitionId", itemValueDefinitionId);
        query.setString("value", value);

        // execute SQL
        List<Long> dataItemIds = query.list();
        return new HashSet<Long>(dataItemIds);
    }

    @SuppressWarnings("unchecked")
    private Set<Long> getDataItemIdsUsingLocaleNames(
            Long dataCategoryId,
            Long itemValueDefinitionId,
            String value) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT i.ID ID ");
        sql.append("FROM ITEM i, ITEM_VALUE iv, LOCALE_NAME ln ");
        sql.append("WHERE i.ID = iv.ITEM_ID ");
        sql.append("AND i.STATUS != :trash ");
        sql.append("AND i.TYPE = 'DI' ");
        sql.append("AND i.DATA_CATEGORY_ID = :dataCategoryId ");
        sql.append("AND iv.ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
        sql.append("AND ln.ENTITY_TYPE='IV' AND ln.ENTITY_ID = iv.ID AND LOCALE = :locale AND ln.NAME = :value");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("ID", Hibernate.LONG);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setLong("dataCategoryId", dataCategoryId);
        query.setLong("itemValueDefinitionId", itemValueDefinitionId);
        query.setString("value", value);
        query.setString("locale", LocaleHolder.getLocale());

        // execute SQL
        List<Long> dataItemIds = query.list();
        if (dataItemIds.isEmpty()) {

            // There are no locale specific values for this locale, so get by default value instead.
            return getDataItemIdsUsingValue(dataCategoryId, itemValueDefinitionId, value);
        }
        return new HashSet<Long>(dataItemIds);
    }

    private boolean isWithinTimeFrame(Date targetStart, Date targetEnd, Date testStart, Date testEnd) {
        boolean result = true;
        if (targetEnd != null) {
            if (!(testStart.before(targetEnd) &&
                    ((testEnd == null) || testEnd.after(targetStart)))) {
                result = false;
            }
        } else {
            if (!((testEnd == null) || testEnd.after(targetStart))) {
                result = false;
            }
        }
        return result;
    }
}