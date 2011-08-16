package com.amee.service.auth;

import com.amee.domain.AMEEEntity;
import com.amee.domain.IAMEEEntity;
import com.amee.domain.IAMEEEntityReference;
import com.amee.domain.auth.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Provides various methods that work with an AuthorizationContext to determine if a request is authorized or not.
 * <p/>
 * Each AuthorizationContext encapsulates a list of principals and a list of AccessSpecifications. The
 * aim is to discover if the principals have the requested access rights to the entities within the AccessSpecification.
 * <p/>
 * The authorization rules are:
 * <p/>
 * - Super-users are always authorized (return true).
 * - Always deny access if there are no AccessSpecifications.
 * - Each AccessSpecification is evaluated in entity hierarchical order (e.g., category -> sub-category -> item).
 * - Principals are evaluated from broader to narrower scope (e.g., organisation -> department -> individual).
 * - PermissionEntries are consolidated from all Permissions for each principal & entity combination.
 * - The PermissionEntries are inherited down the entity hierarchy.
 * - PermissionEntries for later principal & entity combinations override those that are inherited.
 * - Always authorize if an OWN PermissionEntry is present for an entity (return true).
 * - Apply isAuthorized(AccessSpecification, Collection<PermissionEntry>) to each AccessSpecification, return false
 * if not authorized.
 * - Return authorized (true) if isAuthorized is passed for each entity.
 */
@Service
public class AuthorizationService {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private PermissionService permissionService;

    /**
     * Returns true if the supplied AuthorizationContext is considered to be authorized. Internally calls
     * doAuthorization(). This can be called multiple times as the result is cached within AuthorizationContext.
     * <p/>
     * This is used by Resources to authorize incoming requests.
     *
     * @param authorizationContext to consider for authorization
     * @return true if authorize result is allow, otherwise false if result is deny
     */
    public boolean isAuthorized(AuthorizationContext authorizationContext) {
        if (!authorizationContext.hasBeenChecked()) {
            doAuthorization(authorizationContext);
        }
        return authorizationContext.isAuthorized();
    }

    /**
     * Determines if the supplied AuthorizationContext is considered to be authorized. This method
     * should (is) only be executed once for each usage of an AuthorizationContext instance. The
     * AuthorizationContext is updated based on the result of authentication.
     *
     * @param authorizationContext to consider for authorization
     */
    protected void doAuthorization(AuthorizationContext authorizationContext) {

        Boolean allow = null;

        // Work directly with PermissionEntries for the current principals from the AuthorizationContext.
        // It's OK to modify the original Set at this point.
        Set<PermissionEntry> principalEntries = authorizationContext.getEntries();

        // Super-users can do anything. Stop here.
        // NOTE: Jumping out here means authorizationContext & accessSpecifications will not be fully populated.
        if (isSuperUser(authorizationContext.getPrincipals())) {
            log.debug("isAuthorized() - ALLOW (super-user)");
            authorizationContext.setSuperUser(true);
            authorizationContext.setAuthorized(true);
            authorizationContext.addAllowReason("isSuperUser");
            return;
        }

        // Deny if there are no AccessSpecifications. Pretty odd if this happens...
        if (authorizationContext.getAccessSpecifications().isEmpty()) {
            log.debug("isAuthorized() - DENY (not permitted)");
            authorizationContext.setAuthorized(false);
            authorizationContext.addDenyReason("noAccessSpecifications");
            return;
        }

        // Iterate over AccessSpecifications in hierarchical order.
        for (AccessSpecification accessSpecification : authorizationContext.getAccessSpecifications()) {
            // Try to make an authorization decision.
            allow = isAuthorized(authorizationContext, accessSpecification, principalEntries);
            // Was an authorization decision made?
            if (allow != null) {
                break;
            }
        }

        // Was an authorization decision made?
        authorizationContext.setAuthorized(isAuthorized(authorizationContext, allow));
    }

    /**
     * Returns true if the desired access is granted to the last entity in AuthorizationContext. A previously
     * processed AuthorizationContext is required. Will update the cached AccessSpecification for the
     * entity, if it exists.
     * <p/>
     * This is used in resources.
     *
     * @param authorizationContext to consider for authorization
     * @param desired              PermissionEntries specifying access desired
     * @return true if authorize result is allow, otherwise false if result is deny
     */
    public boolean isAuthorized(AuthorizationContext authorizationContext, PermissionEntry... desired) {

        // AuthorizationContext must have already been checked.
        if (!authorizationContext.hasBeenChecked()) {
            throw new IllegalArgumentException("The supplied AuthorizationContext must have already been checked.");
        }

        // Always ALLOW a super-user.
        if (authorizationContext.isSuperUser()) {
            log.debug("isAuthorized() - ALLOW (super-user)");
            authorizationContext.addAllowReason("isSuperUser");
            return true;
        }

        // Work with the last checked AccessSpecification.
        AccessSpecification lastAccessSpecification = authorizationContext.getLastAccessSpecifications();

        // We must have an AccessSpecification to work with.
        if (lastAccessSpecification == null) {
            authorizationContext.addDenyReason("noAccessSpecification");
            return false;
        }

        // Create new AccessSpecification to check for desired PermissionEntries.
        AccessSpecification accessSpecification =
                new AccessSpecification(lastAccessSpecification.getEntityReference(),
                        lastAccessSpecification.getActual(),
                        desired);

        // Try to make an authorization decision.
        // Copy the inherited PermissionEntries for current principals from the AuthorizationContext.
        // It's NOT OK to modify the original Set at this point.
        Boolean allow = isAuthorized(
                authorizationContext,
                accessSpecification,
                new HashSet<PermissionEntry>(authorizationContext.getEntries()));

        // Was an authorization decision made?
        return isAuthorized(authorizationContext, allow);
    }

    /**
     * Returns true if the desired access is granted to the supplied entity. The entity is evaluated as a
     * child of the existing entities within the AuthorizationContext. A previously processed AuthorizationContext
     * is required. Will update the cached AccessSpecification for the entity, if it exists.
     * <p/>
     * This is used by resources & templates.
     *
     * @param authorizationContext to consider for authorization
     * @param entityReference      to authorize access for
     * @param desired              PermissionEntries specifying access desired
     * @return true if authorize result is allow, otherwise false if result is deny
     */
    public boolean isAuthorized(AuthorizationContext authorizationContext, IAMEEEntityReference entityReference, PermissionEntry... desired) {

        Set<PermissionEntry> actual = null;

        // Can we try and re-use the actual entries from an existing AccessSpecification for this entry?
        if (entityReference.getAccessSpecification() != null) {
            actual = entityReference.getAccessSpecification().getActual();
        }

        // Authorize based on existing AuthorizationContext and a new AccessSpecification.
        return isAuthorized(authorizationContext, new AccessSpecification(entityReference, actual, desired));
    }

    /**
     * Returns true if the desired access is granted to the entity, both expressed in AccessSpecification. The
     * entity is evaluated as a child of the existing entities within the AuthorizationContext. A previously
     * processed AuthorizationContext is required.
     *
     * @param authorizationContext to consider for authorization
     * @param accessSpecification  to consider for authorization
     * @return true if authorize result is allow, otherwise false if result is deny
     */
    public boolean isAuthorized(AuthorizationContext authorizationContext, AccessSpecification accessSpecification) {

        // AuthorizationContext must have already been checked.
        if (!authorizationContext.hasBeenChecked()) {
            throw new IllegalArgumentException("The supplied AuthorizationContext must have already been checked.");
        }

        // Was a DENY decision previously made for this AuthorizationContext?
        if (!authorizationContext.isAuthorized()) {
            authorizationContext.addDenyReason("denyDecisionAlreadyMade");
            return false;
        }

        // Always ALLOW super-users.
        if (authorizationContext.isSuperUser()) {
            log.debug("isAuthorized() - ALLOW (super-user)");
            authorizationContext.addAllowReason("isSuperUser");
            return true;
        }

        // Try to make an authorization decision.
        // Copy the inherited PermissionEntries for current principals from the AuthorizationContext.
        // It's NOT OK to modify the original Set at this point.
        Boolean allow = isAuthorized(
                authorizationContext,
                accessSpecification,
                new HashSet<PermissionEntry>(authorizationContext.getEntries()));

        // Was an authorization decision made?
        return isAuthorized(authorizationContext, allow);
    }

    /**
     * Check the supplied allow Boolean. If not null then return the boxed boolean, otherwise return true.
     *
     * @param authorizationContext to consider for authorization
     * @param allow                to be considered
     * @return true if authorize result is allow, otherwise false if result is deny
     */
    protected boolean isAuthorized(AuthorizationContext authorizationContext, Boolean allow) {
        if (allow != null) {
            return allow;
        } else {
            log.debug("isAuthorized() - ALLOW");
            authorizationContext.addAllowReason("noReasonToDeny");
            return true;
        }
    }

    /**
     * Performs authorization for the supplied AccessSpecification.
     * <p/>
     * Conforms to the rules described above.
     *
     * @param authorizationContext to consider for authorization
     * @param accessSpecification  to consider for authorization
     * @param principalEntries     inherited from parent entities
     * @return true if authorize result is allow, false if result is deny or null if the decision is not yet made
     */
    protected Boolean isAuthorized(AuthorizationContext authorizationContext, AccessSpecification accessSpecification, Set<PermissionEntry> principalEntries) {

        List<Permission> permissions;
        List<PermissionEntry> entityEntries;
        Boolean allow = null;

        // Do we need to fetch the actual permission entries for the current principals and entity.
        if (!accessSpecification.hasActual()) {

            // Gather all Permissions for principals for current entity.
            permissions = new ArrayList<Permission>(
                    permissionService.getPermissionsForEntity(
                            authorizationContext, accessSpecification.getEntityReference()));

            // Get list of PermissionEntries for current entity from Permissions.
            entityEntries = getPermissionEntries(permissions);

            // Merge PermissionEntries for current entity with inherited PermissionEntries for current principals.
            mergePermissionEntries(principalEntries, entityEntries);

            // Update the AccessSpecification with the actual PermissionEntries for the current principals related
            // to the current entity.
            accessSpecification.setActual(principalEntries);
        }

        // Owner can do anything.
        if (principalEntries.contains(PermissionEntry.OWN)) {
            log.debug("isAuthorized() - ALLOW (owner)");
            authorizationContext.addAllowReason("isOwner");
            allow = true;
        }

        // Principals must be able to do everything specified.
        if ((allow == null) && !isAuthorizedForAccessSpecification(authorizationContext, accessSpecification, principalEntries)) {
            log.debug("isAuthorized() - DENY (not permitted)");
            allow = false;
        }

        return allow;
    }

    /**
     * Gets a List of PermissionEntries from a Collection of Permissions. The PermissionEntries list contains all
     * the PermissionEntries from within all the Permissions.
     *
     * @param permissions to get PermissionEntries from
     * @return PermissionEntries list
     */
    protected List<PermissionEntry> getPermissionEntries(Collection<Permission> permissions) {
        List<PermissionEntry> entries = new ArrayList<PermissionEntry>();
        for (Permission permission : permissions) {
            entries.addAll(permission.getEntries());
        }
        return entries;
    }

    /**
     * Merges two PermissionEntries Collections. The merge consists of two stages. Firstly, remove PermissionEntries
     * from the target collection where those from the source collection have a different allow flag. Secondly, add
     * all PermissionEntries from the source collection to the target collection.
     *
     * @param targetEntries target collection
     * @param sourceEntries source collection
     */
    protected void mergePermissionEntries(Collection<PermissionEntry> targetEntries, Collection<PermissionEntry> sourceEntries) {
        PermissionEntry pe1;
        Iterator<PermissionEntry> iterator = targetEntries.iterator();
        while (iterator.hasNext()) {
            pe1 = iterator.next();
            for (PermissionEntry pe2 : sourceEntries) {
                if (pe1.getValue().equals(pe2.getValue()) && pe1.getStatus().equals(pe2.getStatus())) {
                    iterator.remove();
                    break;
                }
            }
        }
        targetEntries.addAll(sourceEntries);
    }

    /**
     * Returns true if access is authorized to an entity. The AccessSpecification declares the entity and what
     * kind of access is desired. The PermissionEntry collection declares what kind of access principals are
     * allowed for the entity.
     *
     * @param authorizationContext the current AuthorizationContext
     * @param accessSpecification  specification of access requested to an entity
     * @param principalEntries     PermissionEntries for the current principals
     * @return true if access is authorized
     */
    protected boolean isAuthorizedForAccessSpecification(AuthorizationContext authorizationContext, AccessSpecification accessSpecification, Collection<PermissionEntry> principalEntries) {
        IAMEEEntity entity;
        // We can only authorize if desired PermissionEntries are supplied.
        if (!accessSpecification.getDesired().isEmpty()) {
            // Default to not authorized.
            Boolean authorized = false;
            // Iterate over the desired PermissionEntries specified for the entity.
            for (PermissionEntry desiredEntry : accessSpecification.getDesired()) {
                // Default to not authorized.
                authorized = false;
                // Iterate over PermissionEntries associated with current principals.
                for (PermissionEntry principalEntry : principalEntries) {
                    // Authorized if:
                    // - Both PermissionEntries match by value.
                    // - Principals PermissionEntry is allowed.
                    // - Principals PermissionEntry status matches the entity status.
                    entity = permissionService.getEntity(accessSpecification.getEntityReference());
                    if (desiredEntry.getValue().equals(principalEntry.getValue()) &&
                            principalEntry.isAllow() &&
                            (principalEntry.getStatus().equals(entity.getStatus()))) {
                        // Authorized, no need to continue so break. Most permissive principal PermissionEntry 'wins'.
                        authorized = true;
                        authorizationContext.addAllowReason("matchFor__" + principalEntry.toString() + "__" + entity.toString() + "__" + entity.getStatus().toString());
                        break;
                    } else {
                        // This PermissionEntry *may* cause a DENY.
                        authorizationContext.addDenyReason("noMatchFor__" + principalEntry.toString() + "__" + entity.toString() + "__" + entity.getStatus().toString());
                    }
                }
                // Stop now if not authorized.
                if (!authorized) {
                    authorizationContext.addDenyReason("noMatchFor__" + desiredEntry.toString());
                    break;
                }
            }
            return authorized;
        } else {
            authorizationContext.addDenyReason("noDesiredPermissionEntries");
            return false;
        }
    }

    /**
     * Return true if there is a super-user in the supplied Collection of principals.
     *
     * @param principals to check for presence of super-user
     * @return true if super-user is found
     */
    public boolean isSuperUser(Collection<AMEEEntity> principals) {
        for (AMEEEntity principal : principals) {
            if (isSuperUser(principal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the principal supplied is a User and is a super-user.
     *
     * @param principal to examine
     * @return true if principal is a super-user
     */
    public boolean isSuperUser(AMEEEntity principal) {
        if (User.class.isAssignableFrom(principal.getClass())) {
            if (((User) principal).isSuperUser()) {
                return true;
            }
        }
        return false;
    }
}