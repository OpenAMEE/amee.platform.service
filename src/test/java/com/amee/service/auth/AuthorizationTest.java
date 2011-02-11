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

import com.amee.domain.auth.AccessSpecification;
import com.amee.domain.auth.AuthorizationContext;
import com.amee.domain.auth.PermissionEntry;
import com.amee.service.ServiceTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthorizationTest extends ServiceTest {

    @Autowired
    private AuthorizationService authorizationService;

    @Test
    public void standardUserViewPublicDataCategory() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.GROUP_STANDARD);
        authorizationContext.addPrincipal(serviceData.USER_STANDARD);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_PUBLIC, PermissionEntry.VIEW));
        assertTrue("Standard user should be able to view public data category.", authorizationService.isAuthorized(authorizationContext));
    }

    @Test
    public void standardUserViewPublicDataCategorySub() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.GROUP_STANDARD);
        authorizationContext.addPrincipal(serviceData.USER_STANDARD);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_PUBLIC, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_PUBLIC_SUB, PermissionEntry.VIEW));
        assertTrue("Standard user should be able to view public data sub category.", authorizationService.isAuthorized(authorizationContext));
    }

    @Test
    public void standardUserNotViewPremiumDataCategory() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.GROUP_STANDARD);
        authorizationContext.addPrincipal(serviceData.USER_STANDARD);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_PREMIUM, PermissionEntry.VIEW));
        assertFalse("Standard user should not be able to view premium data category.", authorizationService.isAuthorized(authorizationContext));
    }

    @Test
    public void standardUserNotDeletePremiumDataCategory() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.GROUP_STANDARD);
        authorizationContext.addPrincipal(serviceData.USER_STANDARD);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_PREMIUM, PermissionEntry.VIEW, PermissionEntry.DELETE));
        assertFalse("Standard user should not be able to view premium data category.", authorizationService.isAuthorized(authorizationContext));
    }

    @Test
    public void premiumUserModifyPremiumDataCategory() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.GROUP_STANDARD);
        authorizationContext.addPrincipal(serviceData.GROUP_PREMIUM);
        authorizationContext.addPrincipal(serviceData.USER_PREMIUM);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_PREMIUM, PermissionEntry.VIEW, PermissionEntry.MODIFY));
        assertTrue("Premium user should be able to modify premium data category.", authorizationService.isAuthorized(authorizationContext));
    }

    @Test
    public void superUserDeletePremiumDataCategory() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.USER_SUPER);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_PREMIUM, PermissionEntry.VIEW, PermissionEntry.DELETE));
        assertTrue("Super user should be able to delete premium data category.", authorizationService.isAuthorized(authorizationContext));
    }

    @Test
    public void userViewDeprecatedDataCategory() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.GROUP_STANDARD);
        authorizationContext.addPrincipal(serviceData.USER_STANDARD);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_DEPRECATED, PermissionEntry.VIEW));
        assertTrue("User should be able to view deprecated data category.", authorizationService.isAuthorized(authorizationContext));
    }

    @Test
    public void userNotViewDeprecatedDataCategory() {
        AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.addPrincipal(serviceData.GROUP_STANDARD);
        authorizationContext.addPrincipal(serviceData.GROUP_PREMIUM);
        authorizationContext.addPrincipal(serviceData.USER_PREMIUM);
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_ROOT, PermissionEntry.VIEW));
        authorizationContext.addAccessSpecification(new AccessSpecification(serviceData.DC_DEPRECATED, PermissionEntry.VIEW));
        assertFalse("User should not be able to view deprecated data category.", authorizationService.isAuthorized(authorizationContext));
    }
}
