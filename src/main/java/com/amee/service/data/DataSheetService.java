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

import com.amee.domain.cache.CacheHelper;
import com.amee.domain.data.DataCategory;
import com.amee.domain.sheet.Sheet;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Service for managing data Sheets. This is a Spring bean configured in /conf/applicationContext.xml. See the
 * config file for the list of eternalPaths. 
 */
public class DataSheetService implements Serializable {

    @Autowired
    private DataService dataService;

    private CacheHelper cacheHelper = CacheHelper.getInstance();
    private Set<String> eternalPaths = new HashSet<String>();

    public DataSheetService() {
        super();
    }

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
}