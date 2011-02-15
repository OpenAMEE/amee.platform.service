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
package com.amee.service.definition;

import com.amee.base.domain.ResultsWrapper;
import com.amee.base.transaction.AMEETransaction;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.Pager;
import com.amee.domain.ValueDefinition;
import com.amee.domain.algorithm.AbstractAlgorithm;
import com.amee.domain.algorithm.Algorithm;
import com.amee.domain.algorithm.AlgorithmContext;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.data.ReturnValueDefinition;
import com.amee.platform.search.ItemDefinitionsFilter;
import com.amee.service.BaseService;
import com.amee.service.data.DataService;
import com.amee.service.invalidation.InvalidationMessage;
import com.amee.service.invalidation.InvalidationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("definitionService")
public class DefinitionServiceImpl extends BaseService implements DefinitionService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private InvalidationService invalidationService;

    @Autowired
    private DataService dataService;

    @Autowired
    private DefinitionServiceDAO dao;

    // Events

    @Override
    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void onApplicationEvent(InvalidationMessage invalidationMessage) {
        if ((invalidationMessage.isLocal() || invalidationMessage.isFromOtherInstance()) &&
                invalidationMessage.getObjectType().equals(ObjectType.ID)) {
            log.trace("onApplicationEvent() Handling InvalidationMessage.");
            ItemDefinition itemDefinition = getItemDefinitionByUid(invalidationMessage.getEntityUid());
            if (itemDefinition != null) {
                clearCaches(itemDefinition);
            }
        }
    }

    // Algorithms

    @Override
    public Algorithm getAlgorithmByUid(String uid) {
        return dao.getAlgorithmByUid(uid);
    }

    @Override
    public List<AlgorithmContext> getAlgorithmContexts() {
        return dao.getAlgorithmContexts();
    }

    @Override
    public AlgorithmContext getAlgorithmContextByUid(String uid) {
        AlgorithmContext algorithmContext = dao.getAlgorithmContextByUid(uid);
        return algorithmContext;
    }

    @Override
    public void save(AbstractAlgorithm algorithm) {
        dao.save(algorithm);
    }

    @Override
    public void remove(AbstractAlgorithm algorithm) {
        dao.remove(algorithm);
    }

    // ItemDefinition

    /**
     * Returns the ItemDefinition for the ItemDefinition UID specified. Returns null
     * if the ItemDefinition could not be found.
     *
     * @param uid of the ItemDefinition to fetch
     * @return the ItemDefinition matching the ItemDefinition UID specified
     */
    @Override
    public ItemDefinition getItemDefinitionByUid(String uid) {
        return getItemDefinitionByUid(uid, false);
    }

    /**
     * Returns the ItemDefinition for the ItemDefinition UID specified. Returns null
     * if the ItemDefinition could not be found.
     *
     * @param uid          of the ItemDefinition to fetch
     * @param includeTrash if true will include trashed ItemDefinitions
     * @return the ItemDefinition matching the ItemDefinition UID specified
     */
    @Override
    public ItemDefinition getItemDefinitionByUid(String uid, boolean includeTrash) {
        return dao.getItemDefinitionByUid(uid, includeTrash);
    }

    @Override
    public List<ItemDefinition> getItemDefinitions() {
        ItemDefinitionsFilter filter = new ItemDefinitionsFilter();
        filter.setResultLimit(0);
        filter.setResultStart(0);
        return getItemDefinitions(filter).getResults();
    }

    @Override
    public ResultsWrapper<ItemDefinition> getItemDefinitions(ItemDefinitionsFilter filter) {
        return dao.getItemDefinitions(filter);
    }

    @Override
    public List<ItemDefinition> getItemDefinitions(Pager pager) {
        return dao.getItemDefinitions(pager);
    }

    @Override
    public void save(ItemDefinition itemDefinition) {
        dao.save(itemDefinition);
    }

    @Override
    public void remove(ItemDefinition itemDefinition) {
        dao.remove(itemDefinition);
    }

    /**
     * Invalidate an ItemDefinition. This will send an invalidation message via the
     * InvalidationService and clear the local caches.
     *
     * @param itemDefinition to invalidate
     */
    @Override
    public void invalidate(ItemDefinition itemDefinition) {
        log.info("invalidate() itemDefinition: " + itemDefinition.getUid());
        invalidationService.add(itemDefinition);
        for (IAMEEEntityReference ref : dataService.getDataCategoryReferences(itemDefinition)) {
            invalidationService.add(ref, "indexDataItems");
        }
    }

    /**
     * Clears all caches related to the supplied ItemDefinition.
     *
     * @param itemDefinition to clear caches for
     */
    @Override
    public void clearCaches(ItemDefinition itemDefinition) {
        log.info("clearCaches() itemDefinition: " + itemDefinition.getUid());
        dao.invalidate(itemDefinition);
        // TODO: Include trashed IVDs.
        for (ItemValueDefinition itemValueDefinition : itemDefinition.getItemValueDefinitions()) {
            // TODO: Call service method instead.
            dao.invalidate(itemValueDefinition);
        }
        // TODO: Include trashed RVDs.
        for (ReturnValueDefinition returnValueDefinition : itemDefinition.getReturnValueDefinitions()) {
            // TODO: Call service method instead.
            dao.invalidate(returnValueDefinition);
        }
        // TODO: Algorithms.
        // TODO: ItemDefinition Metadata.
        // TODO: ItemDefinition Locales.
        // TODO: What else? Anything in the index?
    }

    // ItemValueDefinitions

    @Override
    public ItemValueDefinition getItemValueDefinitionByUid(ItemDefinition itemDefinition, String uid) {
        ItemValueDefinition itemValueDefinition = getItemValueDefinitionByUid(uid);
        if (itemValueDefinition.getItemDefinition().equals(itemDefinition)) {
            return itemValueDefinition;
        } else {
            return null;
        }
    }

    @Override
    public ItemValueDefinition getItemValueDefinitionByUid(String uid) {
        return dao.getItemValueDefinitionByUid(uid);
    }

    @Override
    public void save(ItemValueDefinition itemValueDefinition) {
        dao.save(itemValueDefinition);
    }

    @Override
    public void remove(ItemValueDefinition itemValueDefinition) {
        dao.remove(itemValueDefinition);
    }

    // ReturnValueDefinitions

    @Override
    public ReturnValueDefinition getReturnValueDefinitionByUid(ItemDefinition itemDefinition, String uid) {
        ReturnValueDefinition returnValueDefinition = getReturnValueDefinitionByUid(uid);
        if ((returnValueDefinition != null) && returnValueDefinition.getItemDefinition().equals(itemDefinition)) {
            return returnValueDefinition;
        } else {
            return null;
        }
    }

    @Override
    public ReturnValueDefinition getReturnValueDefinitionByUid(String uid) {
        return dao.getReturnValueDefinitionByUid(uid);
    }

    @Override
    public void save(ReturnValueDefinition returnValueDefinition) {
        dao.save(returnValueDefinition);
    }

    /**
     * Set all sibling ReturnValueDefinition defaultType values to false.
     *
     * @param returnValueDefinition the ReturnValueDefinition that is the new default type.
     */
    @Override
    public void unsetDefaultTypes(ReturnValueDefinition returnValueDefinition) {
        if (returnValueDefinition.isDefaultType()) {
            for (ReturnValueDefinition rvd : returnValueDefinition.getItemDefinition().getReturnValueDefinitions()) {
                if (!rvd.equals(returnValueDefinition)) {
                    rvd.setDefaultType(false);
                }
            }
        }
    }

    @Override
    public void remove(ReturnValueDefinition returnValueDefinition) {
        dao.remove(returnValueDefinition);
    }

    // ValueDefinitions

    @Override
    public List<ValueDefinition> getValueDefinitions() {
        return dao.getValueDefinitions();
    }

    @Override
    public List<ValueDefinition> getValueDefinitions(Pager pager) {
        return dao.getValueDefinitions(pager);
    }

    @Override
    public ValueDefinition getValueDefinition(String uid) {
        ValueDefinition valueDefinition = dao.getValueDefinitionByUid(uid);
        return valueDefinition;
    }

    @Override
    public void save(ValueDefinition valueDefinition) {
        dao.save(valueDefinition);
    }

    @Override
    public void remove(ValueDefinition valueDefinition) {
        dao.remove(valueDefinition);
    }
}