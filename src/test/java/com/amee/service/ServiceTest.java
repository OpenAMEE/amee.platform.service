package com.amee.service;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public abstract class ServiceTest {

    @Autowired
    protected ServiceData serviceData;

    @Before
    public void init() {
        serviceData.init();
    }
}
