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

    Algorithm getAlgorithmByUid(String uid);

    List<AlgorithmContext> getAlgorithmContexts();

    AlgorithmContext getAlgorithmContextByUid(String uid);

    void persist(AbstractAlgorithm algorithm);

    void remove(AbstractAlgorithm algorithm);

    ItemDefinition getItemDefinitionByUid(String uid);

    ItemDefinition getItemDefinitionByUid(String uid, boolean includeTrash);

    List<ItemDefinition> getItemDefinitions();

    ResultsWrapper<ItemDefinition> getItemDefinitions(ItemDefinitionsFilter filter);

    List<ItemDefinition> getItemDefinitions(Pager pager);

    void persist(ItemDefinition itemDefinition);

    void remove(ItemDefinition itemDefinition);

    void invalidate(ItemDefinition itemDefinition);

    void clearCaches(ItemDefinition itemDefinition);

    ItemValueDefinition getItemValueDefinitionByUid(ItemDefinition itemDefinition, String uid);

    ItemValueDefinition getItemValueDefinitionByUid(String uid);

    void persist(ItemValueDefinition itemValueDefinition);

    void remove(ItemValueDefinition itemValueDefinition);

    ReturnValueDefinition getReturnValueDefinitionByUid(ItemDefinition itemDefinition, String uid);

    ReturnValueDefinition getReturnValueDefinitionByUid(String uid);

    void persist(ReturnValueDefinition returnValueDefinition);

    void unsetDefaultTypes(ReturnValueDefinition returnValueDefinition);

    void remove(ReturnValueDefinition returnValueDefinition);

    List<ValueDefinition> getValueDefinitions();

    List<ValueDefinition> getValueDefinitions(Pager pager);

    ValueDefinition getValueDefinition(String uid);

    void persist(ValueDefinition valueDefinition);

    void remove(ValueDefinition valueDefinition);
}
