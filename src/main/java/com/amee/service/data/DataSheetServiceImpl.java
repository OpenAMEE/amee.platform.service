package com.amee.service.data;

import com.amee.base.transaction.AMEETransaction;
import com.amee.domain.APIVersion;
import com.amee.domain.DataItemService;
import com.amee.domain.ObjectType;
import com.amee.domain.cache.CacheHelper;
import com.amee.domain.data.DataCategory;
import com.amee.domain.item.data.DataItem;
import com.amee.domain.sheet.Choices;
import com.amee.domain.sheet.Sheet;
import com.amee.service.invalidation.InvalidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DataService dataService;

    @Autowired
    private DataItemService dataItemService;

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
            dataItemService, browser, getEternalPaths().contains(fullPath) ? "DataSheetsEternal" : "DataSheets");
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
