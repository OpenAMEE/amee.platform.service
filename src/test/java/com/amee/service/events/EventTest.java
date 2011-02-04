package com.amee.service.events;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

// TODO: PL-6618
// @RunWith(SpringJUnit4ClassRunner.class)
public class EventTest { // extends ServiceTest {

    @Test
    public void x() {

    }


    // @Autowired
    private PublisherService publisherService;

    // @Autowired
    private SubscriberService subscriberService;

    // @Test
    public void canPublishEvents() {
        assertTrue("SubscriberService message should be null before publishing event.", (subscriberService.getMessage() == null));
        publisherService.publish("Hello!");
        assertTrue("SubscriberService message should not be null after publishing event.", (subscriberService.getMessage() != null));
        assertTrue("SubscriberService message should have correct value after publishing event.", subscriberService.getMessage().equals("Hello!"));
    }
}
