package com.amee.service.auth;

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
