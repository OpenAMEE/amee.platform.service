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
package com.amee.service;

import com.amee.domain.AMEEStatus;
import com.amee.domain.IAMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.ObjectType;
import com.amee.domain.auth.*;
import com.amee.domain.data.DataCategory;
import com.amee.domain.data.ItemDefinition;
import com.amee.domain.item.data.NuDataItem;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServiceData {

    public Group GROUP_STANDARD, GROUP_PREMIUM;
    public User USER_SUPER, USER_STANDARD, USER_PREMIUM;
    public GroupPrincipal GROUP_STANDARD_USER_STANDARD, GROUP_STANDARD_USER_PREMIUM, GROUP_PREMIUM_USER_PREMIUM;
    public ItemDefinition ID_PUBLIC, ID_PREMIUM;
    public DataCategory DC_ROOT, DC_PUBLIC, DC_PUBLIC_SUB, DC_PREMIUM, DC_DEPRECATED;
    public NuDataItem DI_PUBLIC, DI_PREMIUM;
    public Permission PERMISSION_1, PERMISSION_2, PERMISSION_3, PERMISSION_4;
    public Map<IAMEEEntityReference, List<Permission>> PRINCIPAL_TO_PERMISSIONS;
    public Map<ObjectType, Long> ID_MAP;
    public Map<String, IAMEEEntity> ENTITY_MAP;
    public List<DataCategory> creationOrderedCategories = new ArrayList<DataCategory>();
    public List<DataCategory> reverseCreationOrderedCategories = new ArrayList<DataCategory>();

    public void init() {
        initCollections();
        initDefinitions();
        initDataCategories();
        initOrderedDataCategories();
        initDataItems();
        initGroupsAndUsers();
        initPermissions();
    }

    private void initCollections() {
        PRINCIPAL_TO_PERMISSIONS = new HashMap<IAMEEEntityReference, List<Permission>>();
        ID_MAP = new HashMap<ObjectType, Long>();
        ENTITY_MAP = new HashMap<String, IAMEEEntity>();
    }

    private void initDefinitions() {
        ID_PUBLIC = new ItemDefinition("Item Definition Public");
        ID_PREMIUM = new ItemDefinition("Item Definition Premium");
        addEntities(ID_PUBLIC, ID_PREMIUM);
    }

    private void initDataCategories() {
        DC_ROOT = new DataCategory("Root", "root");
        DC_PUBLIC = new DataCategory(DC_ROOT, "DC Public", "dc_public");
        DC_PUBLIC_SUB = new DataCategory(DC_PUBLIC, "DC Public Sub", "dc_public_sub");
        DC_PREMIUM = new DataCategory(DC_ROOT, "DC Premium", "dc_premium");
        DC_DEPRECATED = new DataCategory(DC_ROOT, "DC Deprecated", "dc_deprecated");
        DC_DEPRECATED.setStatus(AMEEStatus.DEPRECATED);
        addEntities(DC_ROOT, DC_PUBLIC, DC_PUBLIC_SUB, DC_PREMIUM, DC_DEPRECATED);
    }

    private void initOrderedDataCategories() {
        DataCategory A1 = new DataCategory(DC_ROOT, "A1", "a1");
        DataCategory A2 = new DataCategory(A1, "A2", "a2");
        DataCategory A3 = new DataCategory(A2, "A3", "a3");
        DataCategory A4 = new DataCategory(A3, "A4", "a4");
        DataCategory A5 = new DataCategory(A3, "A5", "a5");
        DataCategory B1 = new DataCategory(DC_ROOT, "B1", "b1");
        DataCategory B2 = new DataCategory(B1, "B2", "b2");
        DataCategory B3 = new DataCategory(B2, "B3", "b3");
        DataCategory B4 = new DataCategory(B3, "B4", "b4");
        DataCategory B5 = new DataCategory(A3, "B5", "b5");
        creationOrderedCategories.add(A1);
        creationOrderedCategories.add(A2);
        creationOrderedCategories.add(A3);
        creationOrderedCategories.add(A4);
        creationOrderedCategories.add(A5);
        creationOrderedCategories.add(B1);
        creationOrderedCategories.add(B2);
        creationOrderedCategories.add(B3);
        creationOrderedCategories.add(B4);
        creationOrderedCategories.add(B5);
        reverseCreationOrderedCategories.addAll(creationOrderedCategories);
        Collections.reverse(reverseCreationOrderedCategories);
    }

    private void initDataItems() {
        DI_PUBLIC = new NuDataItem(DC_PUBLIC, ID_PUBLIC);
        DI_PREMIUM = new NuDataItem(DC_PREMIUM, ID_PREMIUM);
        addEntities(DI_PUBLIC, DI_PREMIUM);
    }

    private void initGroupsAndUsers() {
        // Groups
        GROUP_STANDARD = new Group("Group Standard");
        GROUP_PREMIUM = new Group("Group Premium");
        addEntities(GROUP_STANDARD, GROUP_PREMIUM);
        // Users
        USER_SUPER = new User("user_super", "password", "User Super");
        USER_SUPER.setType(UserType.SUPER);
        USER_STANDARD = new User("user_standard", "password", "User Standard");
        USER_PREMIUM = new User("user_premium", "password", "User Premium");
        addEntities(USER_SUPER, USER_STANDARD, USER_PREMIUM);
        // Users in Groups
        GROUP_STANDARD_USER_STANDARD = new GroupPrincipal(GROUP_STANDARD, USER_STANDARD);
        GROUP_STANDARD_USER_PREMIUM = new GroupPrincipal(GROUP_STANDARD, USER_PREMIUM);
        GROUP_PREMIUM_USER_PREMIUM = new GroupPrincipal(GROUP_PREMIUM, USER_PREMIUM);
        addEntities(GROUP_STANDARD_USER_STANDARD, GROUP_STANDARD_USER_PREMIUM, GROUP_PREMIUM_USER_PREMIUM);
    }

    private void initPermissions() {
        // Standard group members can view root data category.
        PERMISSION_1 = new Permission(GROUP_STANDARD, DC_ROOT, PermissionEntry.VIEW);
        setId(PERMISSION_1);
        addPermissionToPrincipal(GROUP_STANDARD, PERMISSION_1);
        // Standard group members can not view premium data category.
        PERMISSION_2 = new Permission(GROUP_STANDARD, DC_PREMIUM, PermissionEntry.VIEW_DENY);
        setId(PERMISSION_2);
        addPermissionToPrincipal(GROUP_STANDARD, PERMISSION_2);
        // Premium group members own premium data category.
        PERMISSION_3 = new Permission(GROUP_PREMIUM, DC_PREMIUM, PermissionEntry.OWN);
        setId(PERMISSION_3);
        addPermissionToPrincipal(GROUP_PREMIUM, PERMISSION_3);
        // User can view deprecated data category.
        PERMISSION_4 = new Permission(USER_STANDARD, DC_DEPRECATED, new PermissionEntry("v", true, AMEEStatus.DEPRECATED));
        setId(PERMISSION_4);
        addPermissionToPrincipal(USER_STANDARD, PERMISSION_4);
    }

    private void addPermissionToPrincipal(IAMEEEntityReference principal, Permission permission) {
        List<Permission> permissions = PRINCIPAL_TO_PERMISSIONS.get(principal);
        if (permissions == null) {
            permissions = new ArrayList<Permission>();
            PRINCIPAL_TO_PERMISSIONS.put(principal, permissions);
        }
        permissions.add(permission);
    }

    private void addEntities(IAMEEEntity... entities) {
        for (IAMEEEntity entity : entities) {
            setId(entity);
            ENTITY_MAP.put(entity.getObjectType() + "_" + entity.getEntityUid(), entity);
        }
    }

    private void setId(IAMEEEntity entity) {
        entity.setId(getNextId(entity));
    }

    private Long getNextId(IAMEEEntity entity) {
        Long id = ID_MAP.get(entity.getObjectType());
        if (id == null) {
            id = 0L;
        }
        id++;
        ID_MAP.put(entity.getObjectType(), id);
        return id;
    }
}
