package com.amee.platform.search;

import com.amee.base.validation.ValidationHelper;
import org.apache.lucene.search.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.Set;

@Service
@Scope("prototype")
public class DataCategoryFilterValidationHelper extends ValidationHelper {

    @Autowired
    private DataCategoryFilterValidator validator;

    private DataCategoryFilter dataCategoryFilter;
    private Set<String> allowedFields;

    @Override
    protected void registerCustomEditors(DataBinder dataBinder) {
        dataBinder.registerCustomEditor(Query.class, "uid", new QueryParserEditor("entityUid", SearchService.KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "name", new QueryParserEditor("name"));
        dataBinder.registerCustomEditor(Query.class, "path", new QueryParserEditor("path"));
        dataBinder.registerCustomEditor(Query.class, "fullPath", new QueryParserEditor("fullPath", SearchService.KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "wikiName", new QueryParserEditor("wikiName"));
        dataBinder.registerCustomEditor(Query.class, "wikiDoc", new QueryParserEditor("wikiDoc"));
        dataBinder.registerCustomEditor(Query.class, "provenance", new QueryParserEditor("provenance"));
        dataBinder.registerCustomEditor(Query.class, "authority", new QueryParserEditor("authority"));
        dataBinder.registerCustomEditor(Query.class, "parentUid", new QueryParserEditor("parentUid", SearchService.KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "parentWikiName", new QueryParserEditor("parentWikiName"));
        dataBinder.registerCustomEditor(Query.class, "itemDefinitionUid", new QueryParserEditor("itemDefinitionUid", SearchService.KEYWORD_ANALYZER));
        dataBinder.registerCustomEditor(Query.class, "itemDefinitionName", new QueryParserEditor("itemDefinitionName"));
        dataBinder.registerCustomEditor(Query.class, "tags", new QueryParserEditor("tags"));
    }

    @Override
    public Object getObject() {
        return dataCategoryFilter;
    }

    @Override
    protected Validator getValidator() {
        return validator;
    }

    @Override
    public String getName() {
        return "dataCategoryFilter";
    }

    @Override
    public String[] getAllowedFields() {
        if (allowedFields == null) {
            allowedFields = new HashSet<String>();
            allowedFields.add("uid");
            allowedFields.add("name");
            allowedFields.add("path");
            allowedFields.add("fullPath");
            allowedFields.add("wikiName");
            allowedFields.add("wikiDoc");
            allowedFields.add("provenance");
            allowedFields.add("authority");
            allowedFields.add("parentUid");
            allowedFields.add("parentWikiName");
            allowedFields.add("itemDefinitionUid");
            allowedFields.add("itemDefinitionName");
            allowedFields.add("tags");
            allowedFields.add("resultStart");
            allowedFields.add("resultLimit");
        }
        return allowedFields.toArray(new String[]{});
    }

    public DataCategoryFilter getDataCategoryFilter() {
        return dataCategoryFilter;
    }

    public void setDataCategoryFilter(DataCategoryFilter dataCategoryFilter) {
        this.dataCategoryFilter = dataCategoryFilter;
    }
}