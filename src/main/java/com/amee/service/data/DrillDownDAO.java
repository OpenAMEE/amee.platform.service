package com.amee.service.data;

import static org.hibernate.type.StandardBasicTypes.LONG;
import static org.hibernate.type.StandardBasicTypes.STRING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IDataCategoryReference;
import com.amee.domain.LocaleHolder;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.sheet.Choice;

/**
 * Uses native SQL to perform a 'drill down' into DataItem values.
 * <p/>
 * See {@link com.amee.service.data.DrillDownService} for a description of drill downs.
 * <p/>
 *
 * TODO: Rewrite using JPA JPQL instead of Hibernate Session.
 */
@Service
class DrillDownDAO {

    private final Log log = LogFactory.getLog(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataServiceDAO dataServiceDao;

    /**
     * Retrieves a {@link java.util.List} of {@link com.amee.domain.sheet.Choice}s containing values for a user to select. The value choices
     * are appropriate for the current level within the 'drill down' given the supplied {@link com.amee.domain.data.DataCategory},
     * {@link com.amee.domain.data.ItemValueDefinition) path and selections.
     *
     * @param dc         the {@link com.amee.domain.data.DataCategory} from which {@link com.amee.domain.item.data.DataItem}s will
     *                   be selected (required)
     * @param path       the path of the {@link com.amee.domain.data.ItemValueDefinition) from which to select values
     * @param selections the current user selections for a drill down
     * @return a {@link java.util.List} of {@link com.amee.domain.sheet.Choice}s containing values for a user to select
     */
    public List<Choice> getDataItemValueChoices(
        IDataCategoryReference dc,
        String path,
        List<Choice> selections) {

        Collection<Long> dataItemIds;

        // check arguments
        if ((dc == null) || (!dc.isItemDefinitionPresent()) || (selections == null) || (path == null)) {
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
     * Retrieves a {@link java.util.List} of {@link com.amee.domain.sheet.Choice}s containing values for a user to select. The value choices
     * are appropriate for the current level within the 'drill down' given the supplied {@link com.amee.domain.data.DataCategory},
     * {@link com.amee.domain.data.ItemValueDefinition) path and selections.
     *
     * @param dc         the {@link com.amee.domain.data.DataCategory} from which {@link com.amee.domain.item.data.DataItem}s will
     *                   be selected (required)
     * @param selections the current user selections for a drill down
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
        if (dataItemIds == null) {
            throw new IllegalArgumentException("A required argument is missing.");
        }
        dataItemIds.add(0L);

        // create SQL
        sql = new StringBuilder();
        sql.append("SELECT UID ");
        sql.append("FROM DATA_ITEM ");
        sql.append("WHERE ID IN (:dataItemIds) ");
        sql.append("AND STATUS != :trash");

        // create query
        Session session = (Session) entityManager.getDelegate();
        query = session.createSQLQuery(sql.toString());
        query.addScalar("UID", STRING);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setParameterList("dataItemIds", dataItemIds, LONG);

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

    @SuppressWarnings(value = "unchecked")
    private Collection<String> getDataItemUIDs(Long dataCategoryId, Long itemDefinitionId) {

        // check arguments
        if ((dataCategoryId == null) || (itemDefinitionId == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // create SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT UID ");
        sql.append("FROM DATA_ITEM ");
        sql.append("WHERE STATUS != :trash ");
        sql.append("AND DATA_CATEGORY_ID = :dataCategoryId ");
        sql.append("AND ITEM_DEFINITION_ID = :itemDefinitionId");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("UID", STRING);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setLong("dataCategoryId", dataCategoryId);
        query.setLong("itemDefinitionId", itemDefinitionId);

        // execute SQL
        List<String> dataItemUids = query.list();
        log.debug("getDataItemUIDs() results: " + dataItemUids.size());
        return new HashSet<String>(dataItemUids);
    }

    @SuppressWarnings(value = "unchecked")
    private Collection<Long> getDataItemIds(Long dataCategoryId, Long itemDefinitionId) {

        // check arguments
        if ((dataCategoryId == null) || (itemDefinitionId == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // create SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ID ");
        sql.append("FROM DATA_ITEM ");
        sql.append("WHERE STATUS != :trash ");
        sql.append("AND DATA_CATEGORY_ID = :dataCategoryId ");
        sql.append("AND ITEM_DEFINITION_ID = :itemDefinitionId");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("ID", LONG);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setLong("dataCategoryId", dataCategoryId);
        query.setLong("itemDefinitionId", itemDefinitionId);

        // execute SQL
        List<Long> dataItemIds = query.list();
        log.debug("getDataItemIds() results: " + dataItemIds.size());
        return new HashSet<Long>(dataItemIds);
    }

    /**
     * Get a list of distinct values for a given item value definition ID and collection of data item IDs.
     *
     * @param itemValueDefinitionId
     * @param dataItemIds
     * @return a case-insensitive sorted List of values.
     */
    @SuppressWarnings(value = "unchecked")
    private List<String> getDataItemValues(Long itemValueDefinitionId, Collection<Long> dataItemIds) {

        // Check arguments.
        if ((itemValueDefinitionId == null) || (dataItemIds == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }
        dataItemIds.add(0L);

        // create SQL
        // TODO: Could it be faster to perform two queries and combine the results?
        StringBuilder sql = new StringBuilder();
        sql.append("(SELECT DISTINCT CAST(VALUE AS CHAR) VALUE ");
        sql.append("FROM DATA_ITEM_NUMBER_VALUE ");
        sql.append("WHERE ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
        sql.append("AND STATUS != :trash ");
        sql.append("AND DATA_ITEM_ID IN (:dataItemIds)) ");
        sql.append("UNION ");
        sql.append("(SELECT DISTINCT VALUE ");
        sql.append("FROM DATA_ITEM_TEXT_VALUE ");
        sql.append("WHERE ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
        sql.append("AND STATUS != :trash ");
        sql.append("AND DATA_ITEM_ID IN (:dataItemIds)) ");

        // hsqldb and h2 don't like using LCASE here. Of course MySQL doesn't care.
        //sql.append("ORDER BY LCASE(VALUE) ASC");

        // create query
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("VALUE", STRING);

        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setLong("itemValueDefinitionId", itemValueDefinitionId);
        query.setParameterList("dataItemIds", dataItemIds, LONG);

        // execute SQL
        try {
            List<String> results = query.list();
            log.debug("getDataItemValues() results: " + results.size());

            // Sorting here instead of the query above as hsqldb and h2 don't like using LCASE in the ORDER BY.
            Collections.sort(results, String.CASE_INSENSITIVE_ORDER);
            return results;
        } catch (HibernateException e) {
            log.error("getDataItemValues() Caught HibernateException: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the Data Item IDs for a category that are valid for the given drill down selections.
     *
     * @param dc the DataCategory.
     * @param selections the drill down selections.
     *
     * @return all Data Item IDs that match the drill down selections.
     */
    private Collection<Long> getDataItemIds(IDataCategoryReference dc, List<Choice> selections) {

        // Check arguments.
        if ((dc == null) || (!dc.isItemDefinitionPresent()) || (selections == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }

        // Get the Data Category.
        DataCategory dataCategory = dataServiceDao.getDataCategory(dc);

        // Get all IDs for Data Items in the current Data Category.
        Collection<Long> allCategoryDataItemIds = getDataItemIds(dataCategory.getId(), dataCategory.getItemDefinition().getId());

        // Iterate over selections and fetch DataItem IDs for the selections.
        Set<Long> refinedToSelectionDataItemIds = new HashSet<Long>();
        Collection<Collection<Long>> allPerValueDataItemIds = new ArrayList<Collection<Long>>();
        for (Choice selection : selections) {
            ItemValueDefinition itemValueDefinition = dataCategory.getItemDefinition().getItemValueDefinition(selection.getName());
            if (itemValueDefinition != null) {
                Collection<Long> perValueDataItemIds = getDataItemIds(itemValueDefinition.getId(), allCategoryDataItemIds, selection.getValue());
                allPerValueDataItemIds.add(perValueDataItemIds);
                refinedToSelectionDataItemIds.addAll(perValueDataItemIds);
            } else {
                throw new IllegalArgumentException("Could not locate ItemValueDefinition: " + selection.getName());
            }
        }

        // Reduce all to intersection.
        for (Collection<Long> c : allPerValueDataItemIds) {
            refinedToSelectionDataItemIds.retainAll(c);
        }

        return refinedToSelectionDataItemIds;
    }

    /**
     * Gets a list of IDs of Data Items with a given Item Value Definition and Value for a given list of Data Item IDs.
     *
     * @param itemValueDefinitionId the ID of the Item Value Definition to find.
     * @param categoryDataItemIds a Collection of Data Item IDs to restrict results to (all Data Items in a category).
     * @param value the Data Item value to restrict results to.
     *
     * @return a list of Data Item IDs.
     */
    private Collection<Long> getDataItemIds(Long itemValueDefinitionId, Collection<Long> categoryDataItemIds, String value) {

        Set<Long> dataItemIds;
        if (LocaleHolder.isDefaultLocale()) {
            dataItemIds = getDataItemIdsUsingValue(itemValueDefinitionId, categoryDataItemIds, value);
        } else {
            dataItemIds = getDataItemIdsUsingLocaleNames(itemValueDefinitionId, categoryDataItemIds, value);
        }

        log.debug("getDataItemIds() results: " + dataItemIds.size());

        return dataItemIds;
    }

    /**
     * Queries the database for Data Item Values matching the specified <code>itemValueDefinitionId</code>,
     * <code>categoryDataItemIds</code> and <code>value</code>.  Note that two queries are performed to the
     * <code>DATA_ITEM_NUMBER_VALUE</code> and <code>DATA_ITEM_TEXT_VALUE</code> tables in order to support
     * integration testing using HSQLDB, which is not tolerant of having a string submitted as a condition
     * for a column of type <code>DOUBLE</code>.
     */
    @SuppressWarnings("unchecked")
    private Set<Long> getDataItemIdsUsingValue(Long itemValueDefinitionId, Collection<Long> categoryDataItemIds, String value) {

        // Check arguments.
        if ((itemValueDefinitionId == null) || (categoryDataItemIds == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }
        categoryDataItemIds.add(0L);

        Session session = (Session) entityManager.getDelegate();
        List<Long> result = new ArrayList<Long>();
        SQLQuery query;
        StringBuilder sql;
        
        // Get results from DATA_ITEM_NUMBER_VALUE
        try {    
            // create SQL
            sql = new StringBuilder();
            sql.append("(SELECT DATA_ITEM_ID ID ");
            sql.append("FROM DATA_ITEM_NUMBER_VALUE ");
            sql.append("WHERE DATA_ITEM_ID IN (:dataItemIds) ");
            sql.append("AND STATUS != :trash ");
            sql.append("AND ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
            sql.append("AND VALUE = :value) ");
            
            // create query
            query = session.createSQLQuery(sql.toString());
            query.addScalar("ID", LONG);
    
            // set parameters
            query.setInteger("trash", AMEEStatus.TRASH.ordinal());
            query.setParameterList("dataItemIds", categoryDataItemIds, LONG);
            query.setLong("itemValueDefinitionId", itemValueDefinitionId);
            query.setDouble("value", Double.valueOf(value));
            
            // execute query
            result.addAll(query.list());
            
        } catch (NumberFormatException e) {
            log.debug("Value was not a number: " + value);
        }
        
        // Get results from DATA_ITEM_TEXT_VALUE     
        // create SQL
        sql = new StringBuilder();
        sql.append("(SELECT DATA_ITEM_ID ID ");
        sql.append("FROM DATA_ITEM_TEXT_VALUE ");
        sql.append("WHERE DATA_ITEM_ID IN (:dataItemIds) ");
        sql.append("AND STATUS != :trash ");
        sql.append("AND ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
        sql.append("AND VALUE = :value) ");
        
        // create query
        query = session.createSQLQuery(sql.toString());
        query.addScalar("ID", LONG);
        
        // set parameters
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setParameterList("dataItemIds", categoryDataItemIds, LONG);
        query.setLong("itemValueDefinitionId", itemValueDefinitionId);
        query.setString("value", value);
        
        // execute query
        result.addAll(query.list());  
        
        return new HashSet<Long>(result);
    }

    @SuppressWarnings("unchecked")
    private Set<Long> getDataItemIdsUsingLocaleNames(Long itemValueDefinitionId, Collection<Long> categoryDataItemIds, String value) {

        // Check arguments.
        if ((itemValueDefinitionId == null) || (categoryDataItemIds == null)) {
            throw new IllegalArgumentException("A required argument is missing.");
        }
        categoryDataItemIds.add(0L);

        // Create SQL.
        // Note: HSQL and H2 don't like the fact that we submit a string value for DATA_ITEM_NUMBER_VALUE.VALUE.
        StringBuilder sql = new StringBuilder();
        sql.append("(SELECT dinv.DATA_ITEM_ID ID ");
        sql.append("FROM DATA_ITEM_NUMBER_VALUE dinv, LOCALE_NAME ln ");
        sql.append("WHERE dinv.DATA_ITEM_ID IN (:dataItemIds) ");
        sql.append("AND dinv.STATUS != :trash ");
        sql.append("AND dinv.ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId) ");
        sql.append("UNION ");
        sql.append("(SELECT ditv.DATA_ITEM_ID ID ");
        sql.append("FROM DATA_ITEM_TEXT_VALUE ditv, LOCALE_NAME ln ");
        sql.append("WHERE ditv.DATA_ITEM_ID IN (:dataItemIds) ");
        sql.append("AND ditv.STATUS != :trash ");
        sql.append("AND ditv.ITEM_VALUE_DEFINITION_ID = :itemValueDefinitionId ");
        sql.append("AND ln.ENTITY_TYPE='DITV' AND ln.ENTITY_ID = ditv.ID AND LOCALE = :locale AND ln.NAME = :value)");

        // Create query.
        Session session = (Session) entityManager.getDelegate();
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.addScalar("ID", LONG);

        // Set parameters.
        query.setInteger("trash", AMEEStatus.TRASH.ordinal());
        query.setParameterList("dataItemIds", categoryDataItemIds, LONG);
        query.setLong("itemValueDefinitionId", itemValueDefinitionId);
        query.setString("value", value);
        query.setString("locale", LocaleHolder.getLocale());

        // Execute SQL.
        List<Long> dataItemIds = query.list();
        if (dataItemIds.isEmpty()) {
            // There are no locale specific values for this locale, so get by default value instead.
            return getDataItemIdsUsingValue(itemValueDefinitionId, categoryDataItemIds, value);
        }
        return new HashSet<Long>(dataItemIds);
    }
}