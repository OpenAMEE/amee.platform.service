package com.amee.service.auth;

import com.amee.base.crypto.CryptoException;
import com.amee.base.crypto.InternalCrypto;
import com.amee.domain.auth.User;
import com.amee.domain.environment.Environment;
import com.amee.domain.site.ISite;
import com.amee.service.invalidation.InvalidationMessage;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public interface AuthenticationService extends ApplicationListener<InvalidationMessage> {

    // authToken header and cookie name.
    String AUTH_TOKEN = "authToken";

    User doGuestSignIn();

    String isAuthenticated(ISite site, String authToken, String remoteAddress);

    /**
     * Get the current active user from the supplied AuthToken.
     *
     * @param authToken representing the active user.
     * @return the active user
     */
    User getActiveUser(String authToken);

    /**
     * Authenticates based on the supplied sample user. The sample user must have a username and
     * password set. If authentication is successful the persistent User is returned.
     *
     * @param sampleUser sample User to authenticate against
     * @return the authenticated User
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    User authenticate(User sampleUser);

    String generateAuthToken(User activeUser, String remoteAddress);

    @SuppressWarnings(value = "unchecked")
    User getUserByUid(String uid);

    @SuppressWarnings(value = "unchecked")
    User getUserByUsername(String username);

    /**
     * Clears local caches.
     *
     * @param user the User to clear from the cache.
     */
    void clearCaches(User user);

}
