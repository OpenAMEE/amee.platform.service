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
package com.amee.service.path;

import com.amee.domain.cache.CacheableFactory;
import com.amee.domain.data.DataCategory;
import com.amee.domain.environment.Environment;
import com.amee.domain.path.PathItem;
import com.amee.domain.path.PathItemGroup;
import com.amee.service.data.DataService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EnvironmentPIGFactory implements CacheableFactory {

    private final Log log = LogFactory.getLog(getClass());

    private DataService dataService;
    private Environment environment;

    private EnvironmentPIGFactory() {
        super();
    }

    public EnvironmentPIGFactory(DataService dataService, Environment environment) {
        this();
        this.dataService = dataService;
        this.environment = environment;
    }

    public Object create() {
        log.debug("create()");
        PathItemGroup pathItemGroup = null;
        List<DataCategory> dataCategories = dataService.getDataCategories(environment);
        DataCategory rootDataCategory = findRootDataCategory(dataCategories);
        if (rootDataCategory != null) {
            pathItemGroup = new PathItemGroup(new PathItem(rootDataCategory));
            log.debug("create() - dataCategories.size: " + dataCategories.size());
            addDataCategories(pathItemGroup, dataCategories);
        } else {
            log.error("create() - Root DataCategory not found.");
        }
        return pathItemGroup;
    }

    public String getKey() {
        return environment.getUid();
    }

    public String getCacheName() {
        return "EnvironmentPIGs";
    }

    /**
     * Finds the 'root' DataCategory in the supplied dataCategories List, removes it from the List and
     * returns it. The root DataCategory is the first one found without a parent DataCategory.
     *
     * @param dataCategories list to search
     * @return the root DataCategory
     */
    private DataCategory findRootDataCategory(List<DataCategory> dataCategories) {
        Iterator<DataCategory> iterator = dataCategories.iterator();
        while (iterator.hasNext()) {
            DataCategory dataCategory = iterator.next();
            if (dataCategory.getDataCategory() == null) {
                iterator.remove();
                return dataCategory;
            }
        }
        return null;
    }

    private void addDataCategories(PathItemGroup pathItemGroup, List<DataCategory> dataCategories) {

        log.debug("addDataCategories()");

        PathItem pathItem;
        Map<String, PathItem> pathItems = new HashMap<String, PathItem>();

        // Add root PathItem.
        pathItems.put(pathItemGroup.getRootPathItem().getUid(), pathItemGroup.getRootPathItem());

        // Step One - Create all PathItems.
        for (DataCategory dataCategory : dataCategories) {
            // All DataCategories expected to have a parent.
            if (dataCategory.getDataCategory() != null) {
                pathItems.put(dataCategory.getUid(), new PathItem(dataCategory));
            } else {
                log.warn("addDataCategories() - Parent not set for DC: " + dataCategory.getUid() + " (" + dataCategory.getPath() + ")");
            }
        }

        // Step Two - Bind children & parents.
        for (DataCategory dataCategory : dataCategories) {
            // Find previously created PathItem.
            pathItem = pathItems.get(dataCategory.getUid());
            // Bind current PathItem to parent, if possible.
            if (pathItems.containsKey(dataCategory.getDataCategory().getUid())) {
                log.debug("addDataCategories() - Adding DC: " + dataCategory.getUid() + " (" + dataCategory.getDataCategory().getPath() + "/" + dataCategory.getPath() + ")");
                // Add child PI to parent PI.
                pathItems.get(dataCategory.getDataCategory().getUid()).add(pathItem);
            } else {
                log.warn("addDataCategories() - Parent PathItem not found for DC: " + dataCategory.getUid() + " (" + dataCategory.getPath() + ")");
                // Remove orphaned PIs.
                removeAll(pathItems, pathItem);
            }
        }

        // Step Three - Add PathItems to PathItemGroup.
        pathItemGroup.addAll(pathItems.values());
    }

    private static void removeAll(Map<String, PathItem> pathItems, PathItem pathItem) {
        pathItems.remove(pathItem.getUid());
        for (PathItem pi : pathItem.getChildren()) {
            pathItems.remove(pi.getUid());
        }
    }
}