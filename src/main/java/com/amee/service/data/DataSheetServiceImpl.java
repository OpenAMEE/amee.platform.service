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

import com.amee.base.transaction.AMEETransaction;
import com.amee.domain.APIVersion;
import com.amee.domain.IDataItemService;
import com.amee.domain.ObjectType;
import com.amee.domain.cache.CacheHelper;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.sheet.Choices;
import com.amee.domain.sheet.Sheet;
import com.amee.service.invalidation.InvalidationMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * A Service for managing data Sheets. This is a Spring bean configured in /conf/applicationContext.xml. See the
 * config file for the list of eternalPaths.
 */
public class DataSheetServiceImpl implements DataSheetService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DataService dataService;

    @Autowired
    private IDataItemService dataItemService;

    private CacheHelper cacheHelper = CacheHelper.getInstance();
    private Set<String> eternalPaths = new HashSet<String>();

    // Events

    @Override
    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void onApplicationEvent(InvalidationMessage invalidationMessage) {
        if ((invalidationMessage.isLocal() || invalidationMessage.isFromOtherInstance()) &&
                invalidationMessage.getObjectType().equals(ObjectType.DC)) {
            log.trace("onApplicationEvent() Handling InvalidationMessage.");
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
    @Override
    public void clearCaches(DataCategory dataCategory) {
        log.info("clearCaches() dataCategory: " + dataCategory.getUid());
        removeSheet(dataCategory);
    }

    // Sheets

    @Override
    public Sheet getSheet(DataBrowser browser, String fullPath) {
        DataSheetFactory dataSheetFactory = new DataSheetFactory(
                dataService, browser, getEternalPaths().contains(fullPath) ? "DataSheetsEternal" : "DataSheets");
        return (Sheet) cacheHelper.getCacheable(dataSheetFactory);
    }

    @Override
    public void removeSheet(DataCategory dataCategory) {
        cacheHelper.clearCache("DataSheets", "DataSheet_" + dataCategory.getUid());
        cacheHelper.clearCache("DataSheetsEternal", "DataSheet_" + dataCategory.getUid());
    }

    @Override
    public Set<String> getEternalPaths() {
        return eternalPaths;
    }

    @Override
    public void setEternalPaths(Set<String> eternalPaths) {
        if (eternalPaths != null) {
            this.eternalPaths = eternalPaths;
        }
    }

    // Choices

    @Override
    @Deprecated
    public Choices getUserValueChoices(DataItem dataItem, APIVersion apiVersion) {
        return dataItemService.getUserValueChoices(dataItem, apiVersion);
    }
}