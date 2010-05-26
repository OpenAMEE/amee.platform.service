package com.amee.service.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class SubscriberService implements ApplicationListener {

    private String message = null;

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PubSubEvent) {
            this.message = ((PubSubEvent) event).getMessage();
        }
    }

    public String getMessage() {
        return message;
    }
}
