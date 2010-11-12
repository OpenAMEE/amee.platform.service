package com.amee.service.auth;

import com.amee.domain.AMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.auth.AccessSpecification;
import com.amee.domain.auth.AuthorizationContext;
import com.amee.domain.auth.PermissionEntry;
import com.amee.domain.auth.User;
import com.amee.domain.path.Pathable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A service providing authorization functionality for the configured user and resource.
 */
@Service
@Scope("prototype")
public class ResourceAuthorizationService {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private GroupService groupService;

    private User user;
    private Pathable resource;
    private boolean requireSuperUser = false;
    private AuthorizationContext authorizationContext;

    /**
     * Returns true if the user authorized to get (GET) a representation for the resource.
     *
     * @return true if the user authorized to get (GET) a representation for the resource
     */
    public boolean isAuthorizedForGet() {
        return isAuthorized(getGetAccessSpecifications());
    }

    /**
     * Returns true if the user authorized to accept (PUT) a representation for this resource.
     *
     * @return true if the user authorized to accept (PUT) a representation for this resource
     */
    public boolean isAuthorizedForAccept() {
        return isAuthorized(getAcceptAccessSpecifications());
    }

    /**
     * Returns true if the user authorized to store (POST) a representation for this resource.
     *
     * @return true if the user authorized to store (POST) a representation for this resource
     */
    public boolean isAuthorizedForStore() {
        return isAuthorized(getStoreAccessSpecifications());
    }

    /**
     * Returns true if the user authorized to remove (DELETE) the resource.
     *
     * @return true if the user authorized to remove (DELETE) the resource
     */
    public boolean isAuthorizedForRemove() {
        return isAuthorized(getRemoveAccessSpecifications());
    }

    /**
     * Get the AccessSpecifications for GET requests. Creates an AccessSpecification for each entity
     * from getEntities with VIEW as the PermissionEntry. This specifies that principals must have VIEW permissions
     * for all the entities.
     *
     * @return AccessSpecifications for GET requests
     */
    public List<AccessSpecification> getGetAccessSpecifications() {
        List<AccessSpecification> accessSpecifications = new ArrayList<AccessSpecification>();
        for (IAMEEEntityReference entity : getDistinctEntities()) {
            accessSpecifications.add(new AccessSpecification(entity, PermissionEntry.VIEW));
        }
        return accessSpecifications;
    }

    /**
     * Get the AccessSpecifications for POST requests. Updates the last entry from getGetAccessSpecifications with
     * the CREATE PermissionEntry. This specifies that principals must have VIEW permissions
     * for all the entities and VIEW & CREATE for the last entity.
     *
     * @return AccessSpecifications for POST requests
     */
    public List<AccessSpecification> getAcceptAccessSpecifications() {
        return updateLastAccessSpecificationWithPermissionEntry(getGetAccessSpecifications(), PermissionEntry.CREATE);
    }

    /**
     * Get the AccessSpecifications for PUT requests. Updates the last entry from getGetAccessSpecifications with
     * the MODIFY PermissionEntry. This specifies that principals must have VIEW permissions
     * for all the entities and VIEW & MODIFY for the last entity.
     *
     * @return AccessSpecifications for PUT requests
     */
    public List<AccessSpecification> getStoreAccessSpecifications() {
        return updateLastAccessSpecificationWithPermissionEntry(getGetAccessSpecifications(), PermissionEntry.MODIFY);
    }

    /**
     * Get the AccessSpecifications for DELETE requests. Updates the last entry from getGetAccessSpecifications with
     * the DELETE PermissionEntry. This specifies that principals must have VIEW permissions
     * for all the entities and VIEW & DELETE for the last entity.
     *
     * @return AccessSpecifications for DELETE requests
     */
    public List<AccessSpecification> getRemoveAccessSpecifications() {
        return updateLastAccessSpecificationWithPermissionEntry(getGetAccessSpecifications(), PermissionEntry.DELETE);
    }

    /**
     * Returns true if the request is authorized, otherwise false. AuthorizationService is used to
     * do the authorization based on the supplied AccessSpecifications and the principals from GetPrincipals.
     *
     * @param accessSpecifications to use for authorization
     * @return true if the request is authorized, otherwise false
     */
    public boolean isAuthorized(List<AccessSpecification> accessSpecifications) {
        authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipals(getPrincipals());
        authorizationContext.addAccessSpecifications(accessSpecifications);
        boolean authorized = authorizationService.isAuthorized(authorizationContext);
        if (authorized && isRequireSuperUser() && !authorizationContext.isSuperUser()) {
            authorizationContext.addDenyReason("superUserRequired");
            authorized = false;
        }
        return authorized;
    }

    /**
     * Returns a list of principals involved in authorization. Permissions from these principals will
     * be compared against AccessSpecifications to determine if a request is authorized.
     *
     * @return a list of principals
     */
    public List<AMEEEntity> getPrincipals() {
        List<AMEEEntity> principals = new ArrayList<AMEEEntity>();
        principals.addAll(groupService.getGroupsForPrincipal(getActiveUser()));
        principals.add(getActiveUser());
        return principals;
    }

    /**
     * Returns a list of entities required for authorization for the current resource. The list is
     * in hierarchical order, from general to more specific (e.g., category -> sub-category -> item).
     *
     * @return list of entities required for authorization
     */
    public List<IAMEEEntityReference> getEntities() {
        return resource.getHierarchy();
    }

    /**
     * Returns a de-duped version of the list from getEntities().
     *
     * @return list of entities required for authorization
     */
    public List<IAMEEEntityReference> getDistinctEntities() {
        List<IAMEEEntityReference> entities = new ArrayList<IAMEEEntityReference>();
        for (IAMEEEntityReference entity : getEntities()) {
            if (!entities.contains(entity)) {
                entities.add(entity);
            }
        }
        return entities;
    }

    /**
     * Updates the last AccessSpecification in the supplied list of AccessSpecifications with the PermissionEntry.
     *
     * @param specifications list of AccessSpecifications, of which the last will be updated.
     * @param entry          to add to last AccessSpecification
     * @return the list of AccessSpecifications
     */
    public List<AccessSpecification> updateLastAccessSpecificationWithPermissionEntry(List<AccessSpecification> specifications, PermissionEntry entry) {
        if (!specifications.isEmpty()) {
            specifications.get(specifications.size() - 1).getDesired().add(entry);
        }
        return specifications;
    }

    /**
     * Get the current active signed-in User.
     *
     * @return the current active signed-in User
     */
    public User getActiveUser() {
        return user;
    }

    public void setUserByUid(String userUid) {
        user = authenticationService.getUserByUid(userUid);
        if (user == null) {
            throw new IllegalStateException("User should not be null.");
        }
    }

    public Pathable getResource() {
        return resource;
    }

    public void setResource(Pathable resource) {
        this.resource = resource;
    }

    public boolean isRequireSuperUser() {
        return requireSuperUser;
    }

    public void setRequireSuperUser(boolean requireSuperUser) {
        this.requireSuperUser = requireSuperUser;
    }

    public AuthorizationContext getAuthorizationContext() {
        return authorizationContext;
    }
}
