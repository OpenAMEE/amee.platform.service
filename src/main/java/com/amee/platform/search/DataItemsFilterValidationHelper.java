package com.amee.platform.search;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;

import com.amee.base.validation.BaseValidator;
import com.amee.domain.data.ItemValueDefinition;

@Service
@Scope("prototype")
public class DataItemsFilterValidationHelper extends BaseValidator {

    @Autowired
    private DataItemsFilterValidator validator;

    private DataItemsFilter dataItemsFilter;
    private Set<String> allowedFields;

    @Override
    protected void registerCustomEditors(DataBinder dataBinder) {
        dataBinder.registerCustomEditor(Query.class, "uid", new QueryParserEditor("entityUid", SearchService.KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "name", new QueryParserEditor("name"));
        dataBinder.registerCustomEditor(Query.class, "path", new QueryParserEditor("path", SearchService.LOWER_CASE_KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "wikiDoc", new QueryParserEditor("wikiDoc"));
        dataBinder.registerCustomEditor(Query.class, "provenance", new QueryParserEditor("provenance"));
        dataBinder.registerCustomEditor(Query.class, "categoryUid", new QueryParserEditor("categoryUid", SearchService.KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "categoryWikiName", new QueryParserEditor("categoryWikiName"));
        dataBinder.registerCustomEditor(Query.class, "itemDefinitionUid", new QueryParserEditor("itemDefinitionUid", SearchService.KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "itemDefinitionName", new QueryParserEditor("itemDefinitionName"));
        dataBinder.registerCustomEditor(Query.class, "label", new QueryParserEditor("label"));
        for (ItemValueDefinition ivd : dataItemsFilter.getItemDefinition().getActiveItemValueDefinitions()) {
            if (ivd.isFromData()) {
                if (ivd.isDrillDown()) {
                    dataBinder.registerCustomEditor(
                            Query.class,
                            "queries[" + ivd.getPath() + "]",
                            new QueryParserEditor(ivd.getPath(), SearchService.LOWER_CASE_KEYWORD_ANALYZER));
                } else {
                    if (ivd.isDouble()) {
                        dataBinder.registerCustomEditor(
                                Query.class,
                                "queries[" + ivd.getPath() + "]",
                                new QueryParserEditor(ivd.getPath(), SearchService.STANDARD_ANALYZER, true));
                    } else {
                        dataBinder.registerCustomEditor(
                                Query.class,
                                "queries[" + ivd.getPath() + "]",
                                new QueryParserEditor(ivd.getPath()));
                    }
                }
            }
        }
    }

    @Override
    public Object getObject() {
        return dataItemsFilter;
    }

    @Override
    protected Validator getValidator() {
        return validator;
    }

    @Override
    public String getName() {
        return "dataItemFilter";
    }

    @Override
    public String[] getAllowedFields() {
        if (allowedFields == null) {
            allowedFields = new HashSet<String>();
            allowedFields.add("uid");
            allowedFields.add("name");
            allowedFields.add("path");
            allowedFields.add("wikiDoc");
            allowedFields.add("provenance");
            allowedFields.add("categoryUid");
            allowedFields.add("categoryWikiName");
            allowedFields.add("itemDefinitionUid");
            allowedFields.add("itemDefinitionName");
            allowedFields.add("label");
            for (ItemValueDefinition ivd : dataItemsFilter.getItemDefinition().getActiveItemValueDefinitions()) {
                if (ivd.isFromData()) {
                    allowedFields.add("queries[" + ivd.getPath() + "]");
                }
            }
            allowedFields.add("resultStart");
            allowedFields.add("resultLimit");
        }
        return allowedFields.toArray(new String[]{});
    }

    @Override
    protected void beforeBind(Map<String, String> values) {
        for (ItemValueDefinition ivd : dataItemsFilter.getItemDefinition().getActiveItemValueDefinitions()) {
            if (ivd.isFromData()) {
                this.renameValue(values, ivd.getPath(), "queries[" + ivd.getPath() + "]");
            }
        }
    }

    public DataItemsFilter getDataItemsFilter() {
        return dataItemsFilter;
    }

    public void setDataItemsFilter(DataItemsFilter dataItemsFilter) {
        this.dataItemsFilter = dataItemsFilter;
    }
    
    @Override
    public boolean supports(Class<?> clazz) {
        return validator.supports(clazz);
    }    
}