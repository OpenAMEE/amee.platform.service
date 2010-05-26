/*
 * This file is part of AMEE.
 *
 * Copyright (c) 2007, 2008, 2009 AMEE UK LIMITED (help@amee.com).
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
package com.amee.service.auth;

import com.amee.domain.AMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.auth.Permission;
import com.amee.service.ServiceData;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PermissionServiceDAOMock implements PermissionServiceDAO {

    @Autowired
    protected ServiceData serviceData;

    public Permission getPermissionByUid(String uid) {
        throw new UnsupportedOperationException();
    }

    public void persist(Permission permission) {
        throw new UnsupportedOperationException();
    }

    public void remove(Permission permission) {
        throw new UnsupportedOperationException();
    }

    public List<Permission> getPermissionsForEntity(IAMEEEntityReference entity) {
        throw new UnsupportedOperationException();
    }

    public List<Permission> getPermissionsForPrincipal(IAMEEEntityReference principal, Class entityClass) {
        throw new UnsupportedOperationException();
    }

    public List<Permission> getPermissionsForEntity(IAMEEEntityReference principal, IAMEEEntityReference entity) {
        List<Permission> permissions = new ArrayList<Permission>();
        if (serviceData.PRINCIPAL_TO_PERMISSIONS.containsKey(principal)) {
            for (Permission permission : serviceData.PRINCIPAL_TO_PERMISSIONS.get(principal)) {
                if (permission.getEntityReference().equals(entity)) {
                    permissions.add(permission);
                }
            }
        }
        return permissions;
    }

    public AMEEEntity getEntity(IAMEEEntityReference entityReference) {
        return serviceData.ENTITY_MAP.get(entityReference.getObjectType() + "_" + entityReference.getEntityUid());
    }

    public void trashPermissionsForEntity(IAMEEEntityReference entity) {
        throw new UnsupportedOperationException();
    }
}