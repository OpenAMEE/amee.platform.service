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
package com.amee.calculation.service;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

public class ServiceFinder {

    private Map<String, Service> serviceMap;
    private Map<String, Object> values;
    private ProfileFinder profileFinder;

    public ServiceFinder(Map<String, Service> serviceMap) {
        this.serviceMap = serviceMap;
    }

    public Service getService(String serviceName) {
        Service service = serviceMap.get(serviceName);
        service.setValues(values);
        service.setProfileFinder(profileFinder);
        return service;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public void setProfileFinder(ProfileFinder profileFinder) {
        this.profileFinder = profileFinder;
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext-algorithmServices.xml");
        ServiceFinder service = (ServiceFinder) ctx.getBean("serviceFinder");
        Service rs = service.getService("train-route-finder-service");
        System.out.println(rs.invoke());
    }
}