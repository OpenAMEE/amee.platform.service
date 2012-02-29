package com.amee.service.auth;

import com.amee.base.crypto.CryptoException;
import com.amee.base.crypto.InternalCrypto;
import com.amee.base.transaction.AMEETransaction;
import com.amee.domain.ObjectType;
import com.amee.domain.auth.User;
import com.amee.domain.environment.Environment;
import com.amee.domain.site.ISite;
import com.amee.service.invalidation.InvalidationMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final static Log log = LogFactory.getLog(AuthenticationServiceImpl.class);

    @Autowired
    private AuthenticationDAO authenticationDao;

    @Override
    public User doGuestSignIn() {
        return getUserByUsername("guest");
    }

    @Override
    public String isAuthenticated(ISite site, String authToken, String remoteAddress) {

        User activeUser;
        Map<String, String> values;
        boolean remoteAddressCheckPassed = false;
        boolean maxAuthDurationCheckPassed = false;
        boolean maxAuthIdleCheckPassed = false;
        long now = System.currentTimeMillis();
        String oldAuthToken;

        // has authToken been supplied?
        if (authToken != null) {

            log.debug("authToken supplied");

            // must have a Site object
            if (site == null) {
                log.error("Site object missing.");
                throw new RuntimeException("Site object missing.");
            }

            // get authToken values
            oldAuthToken = authToken;
            authToken = AuthToken.decryptToken(authToken);
            values = AuthToken.explodeToken(authToken);

            // check remote address
            if (site.isCheckRemoteAddress()) {
                String remoteAddressHash = values.get(AuthToken.REMOTE_ADDRESS_HASH);
                if (remoteAddressHash != null) {
                    try {
                        if (remoteAddress.hashCode() == Integer.valueOf(remoteAddressHash)) {
                            log.debug("remote address check passed: " + remoteAddress);
                            remoteAddressCheckPassed = true;
                        }
                    } catch (NumberFormatException e) {
                        // swallow
                    }
                }
            } else {
                // ignore remote address check
                remoteAddressCheckPassed = true;
            }
            if (!remoteAddressCheckPassed) {
                log.debug("auth NOT authenticated, remote address check failed: " + remoteAddress);
                return null;
            }

            // check auth duration
            if (site.getMaxAuthDuration() >= 0) {
                try {
                    Long created = new Long(values.get(AuthToken.CREATED));
                    maxAuthDurationCheckPassed = (created + site.getMaxAuthDuration() > now);
                } catch (NumberFormatException e) {
                    // swallow
                }
            } else {
                // ignore max auth duration check
                maxAuthDurationCheckPassed = true;
            }
            if (!maxAuthDurationCheckPassed) {
                log.debug("auth NOT authenticated, max auth duration check failed");
                return null;
            }

            // check auth idle
            if (site.getMaxAuthIdle() >= 0) {
                try {
                    Long touched = new Long(values.get(AuthToken.MODIFIED));
                    maxAuthIdleCheckPassed = (touched + site.getMaxAuthIdle() > now);
                } catch (NumberFormatException e) {
                    // swallow
                }
            } else {
                // ignore max auth idle check
                maxAuthIdleCheckPassed = true;
            }
            if (!maxAuthIdleCheckPassed) {
                log.debug("auth NOT authenticated, max auth idle check failed");
                return null;
            }

            // get and check auth
            String userUid = values.get(AuthToken.USER_UID);
            if (userUid != null) {
                activeUser = getUserByUid(userUid);
                if (activeUser != null) {
                    log.debug("auth authenticated and signed in: " + activeUser.getUsername());
                    Long touched = new Long(values.get(AuthToken.MODIFIED));
                    // only touch token if older than 60 seconds (60*1000ms)
                    if (now > (touched + 60 * 1000)) {
                        return AuthToken.touchToken(authToken);
                    } else {
                        return oldAuthToken;
                    }
                }
            }

        } else {
            log.debug("authToken NOT supplied");
        }

        log.debug("auth NOT authenticated");
        return null;
    }

    @Override
    public User getActiveUser(String authToken) {

        if (authToken == null) {
            throw new IllegalArgumentException("AuthToken String must not be null.");
        }

        // get authToken values
        authToken = AuthToken.decryptToken(authToken);
        Map<String, String> values = AuthToken.explodeToken(authToken);

        // get and check auth
        String userUid = values.get(AuthToken.USER_UID);
        if (userUid != null) {
            return getUserByUid(userUid);
        } else {
            log.debug("getActiveUser() - active user NOT found");
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public User authenticate(User sampleUser) {
        // Try to find User based on 'sampleUser' User 'template'.
        User activeUser = getUserByUsername(sampleUser.getUsername());
        if (activeUser != null) {
            if (activeUser.getPassword().equals(sampleUser.getPassword())) {
                log.debug("authenticate() - User authenticated and signed in: " + sampleUser.getUsername());
                return activeUser;
            } else {
                log.debug("authenticate() - User NOT authenticated, bad password: " + sampleUser.getUsername());
                return null;
            }
        } else {
            log.debug("authenticate() - User NOT authenticated, not found: " + sampleUser.getUsername());
            return null;
        }
    }

    @Override
    public String generateAuthToken(User activeUser, String remoteAddress) {
        return AuthToken.createToken(activeUser, remoteAddress);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public User getUserByUid(String uid) {
        return authenticationDao.getUserByUid(uid);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public User getUserByUsername(String username) {
        return authenticationDao.getUserByUsername(username);
    }

    /**
     * Handles invalidation messages for users. This will clear the local cache.
     *
     * @param invalidationMessage the message received.
     */
    @AMEETransaction
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public void onApplicationEvent(InvalidationMessage invalidationMessage) {
        if ((invalidationMessage.isLocal() || invalidationMessage.isFromOtherInstance()) &&
            invalidationMessage.getObjectType().equals(ObjectType.USR)) {
            log.trace("onApplicationEvent() Handling InvalidationMessage.");
            User user = getUserByUid(invalidationMessage.getEntityUid());
            if (user != null) {
                clearCaches(user);
            }
        }
    }

    @Override
    public void clearCaches(User user) {
        log.info("clearCaches() user: " + user.getUid());
        authenticationDao.invalidate(user);
    }

    public static class AuthToken implements Serializable {

        public final static String ENVIRONMENT_UID = "en";
        public final static String USER_UID = "us";
        public final static String REMOTE_ADDRESS_HASH = "ra";
        public final static String CREATED = "cr";
        public final static String MODIFIED = "mo";

        public static Map<String, String> explodeToken(String token) {
            String[] pairsArr;
            String[] pairArr;
            String name;
            String value;
            Map<String, String> values = new HashMap<String, String>();
            pairsArr = token.split("\\|"); // | is the delimiter in this regex
            for (String pair : pairsArr) {
                pairArr = pair.split("=");
                if (pairArr.length > 0) {
                    name = pairArr[0];
                    if (pairArr.length > 1) {
                        value = pairArr[1];
                    } else {
                        value = "";
                    }
                    values.put(name, value);
                }
            }
            return values;
        }

        public static String implodeToken(Map<String, String> values) {
            StringBuilder sb = new StringBuilder("");
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("|");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            return sb.toString();
        }

        public static String createToken(User user, String remoteAddress) {
            String now = "" + System.currentTimeMillis();
            Map<String, String> values = new HashMap<String, String>();
            values.put(ENVIRONMENT_UID, Environment.ENVIRONMENT.getUid());
            values.put(USER_UID, user.getUid());
            values.put(REMOTE_ADDRESS_HASH, "" + remoteAddress.hashCode());
            values.put(CREATED, now);
            values.put(MODIFIED, now);
            return encryptToken(implodeToken(values));
        }

        public static String touchToken(String token) {
            String now = "" + System.currentTimeMillis();
            Map<String, String> values = explodeToken(token);
            values.put(MODIFIED, now);
            return encryptToken(implodeToken(values));
        }

        public static String decryptToken(String token) {
            try {
                return InternalCrypto.decrypt(token);
            } catch (CryptoException e) {
                log.warn("Caught CryptoException: " + e.getMessage(), e);
                // TODO: Do something nice now.
                return "";
            }
        }

        public static String encryptToken(String token) {
            try {
                return InternalCrypto.encrypt(token);
            } catch (CryptoException e) {
                log.warn("Caught CryptoException: " + e.getMessage(), e);
                // TODO: Do something nice now.
                return "";
            }
        }
    }
}
