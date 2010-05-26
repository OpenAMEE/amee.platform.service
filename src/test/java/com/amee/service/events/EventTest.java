package com.amee.service.events;

import com.amee.service.ServiceTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class EventTest extends ServiceTest {

    @Autowired
    private PublisherService publisherService;

    @Autowired
    private SubscriberService subscriberService;

    @Test
    public void canPublishEvents() {
        assertTrue("SubscriberService message should be null before publishing event.", (subscriberService.getMessage() == null));
        publisherService.publish("Hello!");
        assertTrue("SubscriberService message should not be null after publishing event.", (subscriberService.getMessage() != null));
        assertTrue("SubscriberService message should have correct value after publishing event.", subscriberService.getMessage().equals("Hello!"));
    }
}
