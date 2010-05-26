package com.amee.service.events;

import org.springframework.context.ApplicationEvent;

public class PubSubEvent extends ApplicationEvent {

    private String message = null;

    public PubSubEvent(Object source) {
        super(source);
    }

    public PubSubEvent(Object source, String message) {
        this(source);
        this.setMessage(message);
    }

    public String getMessage() {
        return message;
    }

    private void setMessage(String message) {
        this.message = message;
    }
}
