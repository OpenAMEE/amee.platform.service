package com.amee.service.data;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.cache.CacheHelper;
import com.amee.domain.sheet.Choice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SqlDrillDownService extends AbstractDrillDownService {

    @Autowired
    private DrillDownDAO drillDownDao;

    @Override
    @SuppressWarnings("unchecked")
    protected List<Choice> getDataItemChoices(
        IDataCategoryReference dataCategory, List<Choice> selections, List<Choice> drillDownChoices) {

        // Get Choices and sort.
        List<Choice> choices = ((List<Choice>) cacheHelper.getCacheable(
                new DrillDownFactory(drillDownDao, dataCategory, selections, drillDownChoices)));
        Collections.sort(choices);
        return choices;
    }
}