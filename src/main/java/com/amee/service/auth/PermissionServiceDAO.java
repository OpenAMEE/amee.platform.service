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
import com.amee.domain.IAMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.auth.Permission;

import java.util.List;

public interface PermissionServiceDAO {

    Permission getPermissionByUid(String uid);

    void persist(Permission permission);

    void remove(Permission permission);

    List<Permission> getPermissionsForEntity(IAMEEEntityReference entity);

    List<Permission> getPermissionsForPrincipal(IAMEEEntityReference principal, Class entityClass);

    List<Permission> getPermissionsForEntity(IAMEEEntityReference principal, IAMEEEntityReference entity);

    /**
     * Fetch the entity referenced by the IAMEEEntityReference from the database.
     *
     * @param entityReference to fetch
     * @return fetched entity
     */
    IAMEEEntity getEntity(IAMEEEntityReference entityReference);

    void trashPermissionsForEntity(IAMEEEntityReference entity);
}
