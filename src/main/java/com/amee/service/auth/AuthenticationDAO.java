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

import com.amee.domain.AMEEStatus;
import com.amee.domain.auth.User;
import com.amee.domain.environment.Environment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class AuthenticationDAO {

    private final Log log = LogFactory.getLog(getClass());

    private static final String CACHE_REGION = "query.authenticationService";

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings(value = "unchecked")
    public User getUserByUid(Environment environment, String uid) {
        List<User> users = entityManager.createQuery(
                "SELECT DISTINCT u " +
                        "FROM User u " +
                        "WHERE u.environment.id = :environmentId " +
                        "AND u.uid = :userUid " +
                        "AND u.status != :trash")
                .setParameter("environmentId", environment.getId())
                .setParameter("userUid", uid)
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        if (users.size() == 1) {
            log.debug("auth found: " + uid);
            return users.get(0);
        }
        log.debug("auth NOT found: " + uid);
        return null;
    }

    @SuppressWarnings(value = "unchecked")
    public User getUserByUsername(Environment environment, String username) {
        List<User> users = entityManager.createQuery(
                "SELECT DISTINCT u " +
                        "FROM User u " +
                        "WHERE u.environment.id = :environmentId " +
                        "AND u.username = :username " +
                        "AND u.status != :trash")
                .setParameter("environmentId", environment.getId())
                .setParameter("username", username)
                .setParameter("trash", AMEEStatus.TRASH)
                .setHint("org.hibernate.cacheable", true)
                .setHint("org.hibernate.cacheRegion", CACHE_REGION)
                .getResultList();
        if (users.size() == 1) {
            log.debug("auth found: " + username);
            return users.get(0);
        }
        log.debug("auth NOT found: " + username);
        return null;
    }
}
