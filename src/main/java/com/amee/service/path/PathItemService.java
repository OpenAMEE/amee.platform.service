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

import com.amee.domain.ObjectType;
import com.amee.domain.cache.CacheHelper;
import com.amee.domain.data.DataCategory;
import com.amee.domain.environment.Environment;
import com.amee.domain.path.PathItem;
import com.amee.domain.path.PathItemGroup;
import com.amee.service.data.DataService;
import com.amee.service.invalidation.InvalidationMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PathItemService implements ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DataService dataService;

    private CacheHelper cacheHelper = CacheHelper.getInstance();

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
            DataCategory dataCategory = dataService.getDataCategoryByUid(invalidationMessage.getEntityUid(), true);
            if (dataCategory != null) {
                update(dataCategory);
            } else {
                remove(invalidationMessage.getEntityUid());
            }
        }
    }

    // Manage PathItems.

    public void update(DataCategory dataCategory) {
        PathItemGroup pig = getPathItemGroup();
        PathItem pathItem = pig.findByUId(dataCategory.getUid());
        if (pathItem != null) {
            if (dataCategory.isActive()) {
                pathItem.update(dataCategory);
            } else {
                remove(dataCategory.getUid());
            }
        } else {
            if (dataCategory.isActive() && (dataCategory.getDataCategory() != null)) {
                // TODO: This won't add children.
                // TODO: This won't handle missing parents.
                // TODO: Ticket - https://jira.amee.com/browse/PL-1788 
                PathItem parentPathItem = pig.findByUId(dataCategory.getDataCategory().getUid());
                if (parentPathItem != null) {
                    pathItem = new PathItem(dataCategory);
                    parentPathItem.add(pathItem);
                } else {
                    log.warn("update() Could not find parent PathItem.");
                }
            } else {
                log.warn("update() DataCategory is not active or has no parent.");
            }
        }
    }

    public void remove(String uid) {
        PathItemGroup pig = getPathItemGroup();
        pig.remove(pig.findByUId(uid));
    }

    public PathItemGroup getPathItemGroup() {
        return (PathItemGroup) cacheHelper.getCacheable(new PIGFactory(dataService));
    }

    public void removePathItemGroup() {
        cacheHelper.remove("PIGs", Environment.ENVIRONMENT.getUid());
    }
}
