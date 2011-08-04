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