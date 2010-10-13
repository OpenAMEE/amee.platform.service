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
package com.amee.service.data;

import com.amee.domain.APIVersion;
import com.amee.domain.ObjectType;
import com.amee.domain.cache.CacheHelper;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.DataItem;
import com.amee.domain.data.ItemValue;
import com.amee.domain.data.ItemValueDefinition;
import com.amee.domain.sheet.Choice;
import com.amee.domain.sheet.Choices;
import com.amee.domain.sheet.Sheet;
import com.amee.service.BaseService;
import com.amee.service.invalidation.InvalidationMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Service for managing data Sheets. This is a Spring bean configured in /conf/applicationContext.xml. See the
 * config file for the list of eternalPaths.
 */
public class DataSheetService extends BaseService implements ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DataService dataService;

    private CacheHelper cacheHelper = CacheHelper.getInstance();
    private Set<String> eternalPaths = new HashSet<String>();

    // Events

    public void onApplicationEvent(ApplicationEvent event) {
        if (InvalidationMessage.class.isAssignableFrom(event.getClass())) {
            onInvalidationMessage((InvalidationMessage) event);
        }
    }

    @Transactional(readOnly = true)
    private void onInvalidationMessage(InvalidationMessage invalidationMessage) {
        if ((invalidationMessage.isLocal() || invalidationMessage.isFromOtherInstance()) &&
                invalidationMessage.getObjectType().equals(ObjectType.DC)) {
            log.debug("onInvalidationMessage() Handling InvalidationMessage.");
            DataCategory dataCategory = dataService.getDataCategoryByUid(invalidationMessage.getEntityUid(), null);
            if (dataCategory != null) {
                clearCaches(dataCategory);
            }
        }
    }

    /**
     * Clears all caches related to the supplied DataCategory.
     *
     * @param dataCategory to clear caches for
     */
    public void clearCaches(DataCategory dataCategory) {
        log.info("clearCaches() dataCategory: " + dataCategory.getUid());
        removeSheet(dataCategory);
    }

    // Sheets

    public Sheet getSheet(DataBrowser browser, String fullPath) {
        DataSheetFactory dataSheetFactory = new DataSheetFactory(
                dataService, browser, getEternalPaths().contains(fullPath) ? "DataSheetsEternal" : "DataSheets");
        return (Sheet) cacheHelper.getCacheable(dataSheetFactory);
    }

    public void removeSheet(DataCategory dataCategory) {
        cacheHelper.clearCache("DataSheets", "DataSheet_" + dataCategory.getUid());
        cacheHelper.clearCache("DataSheetsEternal", "DataSheet_" + dataCategory.getUid());
    }

    public Set<String> getEternalPaths() {
        return eternalPaths;
    }

    public void setEternalPaths(Set<String> eternalPaths) {
        if (eternalPaths != null) {
            this.eternalPaths = eternalPaths;
        }
    }

    // Choices

    @SuppressWarnings(value = "unchecked")
    public Choices getUserValueChoices(DataItem dataItem, APIVersion apiVersion) {
        List<Choice> userValueChoices = new ArrayList<Choice>();
        for (ItemValueDefinition ivd : dataItem.getItemDefinition().getItemValueDefinitions()) {
            if (ivd.isFromProfile() && ivd.isValidInAPIVersion(apiVersion)) {
                // start default value with value from ItemValueDefinition
                String defaultValue = ivd.getValue();
                // next give DataItem a chance to set the default value, if appropriate
                if (ivd.isFromData()) {
                    ItemValue dataItemValue = dataItem.getItemValue(ivd.getPath());
                    if ((dataItemValue != null) && (dataItemValue.getValue().length() > 0)) {
                        defaultValue = dataItemValue.getValue();
                    }
                }
                // create Choice
                userValueChoices.add(new Choice(ivd.getPath(), defaultValue));
            }
        }
        return new Choices("userValueChoices", userValueChoices);
    }
}