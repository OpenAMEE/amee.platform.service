package com.amee.service.data;

import com.amee.domain.IDataCategoryReference;
import com.amee.domain.LocaleHolder;
import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.sheet.Choice;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DrillDownFactory implements CacheableFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DrillDownDAO drillDownDao;
    private IDataCategoryReference dataCategory;
    private List<Choice> selections;
    private List<Choice> drillDownChoices;
    private String key;

    private DrillDownFactory() {
        super();
    }

    public DrillDownFactory(
            DrillDownDAO drillDownDao,
            IDataCategoryReference dataCategory,
            List<Choice> selections,
            List<Choice> drillDownChoices) {
        this();
        this.drillDownDao = drillDownDao;
        this.dataCategory = dataCategory;
        this.selections = selections;
        this.drillDownChoices = drillDownChoices;
    }

    // TODO: give choices from itemValueDefinition priority?
    public Object create() {
        log.debug("create() cache: " + getCacheName() + " key: " + getKey());
        // have we reached the end of the choices list?
        if (drillDownChoices.size() > 0) {
            // get DataItem value choice list
            return drillDownDao.getDataItemValueChoices(
                    dataCategory, drillDownChoices.get(0).getName(), selections);
        } else {
            // get DataItem UID choice list
            return drillDownDao.getDataItemUIDChoices(dataCategory, selections);
        }
    }

    public String getKey() {
        if (key == null) {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append("DrillDown_");
            keyBuilder.append(dataCategory.getEntityUid());
            for (Choice selection : selections) {
                keyBuilder.append("_SL_");
                if (StringUtils.isNotBlank(selection.getValue())) {
                    keyBuilder.append(selection.getValue().hashCode());
                } else {
                    keyBuilder.append("BLANK");
                }
            }
            keyBuilder.append("__L__");
            keyBuilder.append(LocaleHolder.getLocale());

            key = keyBuilder.toString();
        }
        return key;
    }

    public String getCacheName() {
        return "DrillDownChoices";
    }
}
