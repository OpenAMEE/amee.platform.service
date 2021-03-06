package com.amee.service.auth;

import com.amee.domain.AMEEStatus;
import com.amee.domain.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class AuthenticationDAO {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CACHE_REGION = "query.authenticationService";

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings(value = "unchecked")
    public User getUserByUid(String uid) {
        List<User> users = entityManager.createQuery(
                "SELECT DISTINCT u " +
                        "FROM User u " +
                        "WHERE u.uid = :userUid " +
                        "AND u.status != :trash")
                .setParameter("userUid", uid)
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        if (users.size() == 1) {
            log.debug("auth found: {}",  uid);
            return users.get(0);
        }
        log.debug("auth NOT found: {}", uid);
        return null;
    }

    @SuppressWarnings(value = "unchecked")
    public User getUserByUsername(String username) {
        List<User> users = entityManager.createQuery(
                "SELECT DISTINCT u " +
                        "FROM User u " +
                        "WHERE u.username = :username " +
                        "AND u.status != :trash")
                .setParameter("username", username)
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        if (users.size() == 1) {
            log.debug("auth found: {}", username);
            return users.get(0);
        }
        log.debug("auth NOT found: {}", username);
        return null;
    }

    /**
     * Clears the hibernate cache for the given User.
     *
     * @param user the User to clear from the cache.
     */
    public void invalidate(User user) {
        log.debug("invalidate() {}", user);
        ((Session) entityManager.getDelegate()).getSessionFactory().getCache().evictEntity(User.class, user.getId());
    }
}
