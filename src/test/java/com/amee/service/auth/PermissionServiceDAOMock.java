package com.amee.service.auth;

import com.amee.domain.IAMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.auth.Permission;
import com.amee.service.ServiceData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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

    public IAMEEEntity getEntity(IAMEEEntityReference entityReference) {
        return serviceData.ENTITY_MAP.get(entityReference.getObjectType() + "_" + entityReference.getEntityUid());
    }

    public void trashPermissionsForEntity(IAMEEEntityReference entity) {
        throw new UnsupportedOperationException();
    }
}