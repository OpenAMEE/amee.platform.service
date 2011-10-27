package com.amee.service.definition;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.Pager;
import com.amee.domain.ValueDefinition;
import com.amee.domain.algorithm.AbstractAlgorithm;
import com.amee.domain.algorithm.Algorithm;
import com.amee.domain.algorithm.AlgorithmContext;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.data.ReturnValueDefinition;
import com.amee.platform.search.ItemDefinitionsFilter;
import com.amee.service.invalidation.InvalidationMessage;
import org.springframework.context.ApplicationListener;

import java.util.List;

public interface DefinitionService extends ApplicationListener<InvalidationMessage> {

    public Algorithm getAlgorithmByUid(ItemDefinition itemDefinition, String uid);

    public Algorithm getAlgorithmByUid(String uid);

    public List<AlgorithmContext> getAlgorithmContexts();

    public AlgorithmContext getAlgorithmContextByUid(String uid);

    public void persist(AbstractAlgorithm algorithm);

    public void remove(AbstractAlgorithm algorithm);

    public ItemDefinition getItemDefinitionByUid(String uid);

    public ItemDefinition getItemDefinitionByUid(String uid, boolean includeTrash);

    public List<ItemDefinition> getItemDefinitions();

    public ResultsWrapper<ItemDefinition> getItemDefinitions(ItemDefinitionsFilter filter);

    public List<ItemDefinition> getItemDefinitions(Pager pager);

    public void persist(ItemDefinition itemDefinition);

    public void remove(ItemDefinition itemDefinition);

    /**
     * Invalidate an ItemDefinition. All DataCategories that use the ItemDefinition will also be invalidated and
     * their DataItems will be re-indexed.
     *
     * @param itemDefinition the ItemDefinition to invalidate.
     */
    public void invalidate(ItemDefinition itemDefinition);

    public void clearCaches(ItemDefinition itemDefinition);

    public ItemValueDefinition getItemValueDefinitionByUid(ItemDefinition itemDefinition, String uid);

    public ItemValueDefinition getItemValueDefinitionByUid(String uid);

    public void persist(ItemValueDefinition itemValueDefinition);

    public void remove(ItemValueDefinition itemValueDefinition);

    public ReturnValueDefinition getReturnValueDefinitionByUid(ItemDefinition itemDefinition, String uid);

    public ReturnValueDefinition getReturnValueDefinitionByUid(String uid);

    public void persist(ReturnValueDefinition returnValueDefinition);

    public void unsetDefaultTypes(ReturnValueDefinition returnValueDefinition);

    public void remove(ReturnValueDefinition returnValueDefinition);

    public List<ValueDefinition> getValueDefinitions();

    public List<ValueDefinition> getValueDefinitions(Pager pager);

    public ValueDefinition getValueDefinition(String uid);

    public void persist(ValueDefinition valueDefinition);

    public void remove(ValueDefinition valueDefinition);
}
