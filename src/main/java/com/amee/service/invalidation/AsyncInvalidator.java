package com.amee.service.invalidation;

import com.amee.messaging.MessageService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AsyncInvalidator {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private InvalidationService invalidationService;

    @Autowired
    private MessageService messageService;

    @Async
    public void invalidate(Set<InvalidationMessage> invalidationMessagesCopy) {
        int messageCount = 0;
        int messageSize = invalidationMessagesCopy.size();
        for (InvalidationMessage invalidationMessage : invalidationMessagesCopy) {
            log.trace("Invalidating message " + messageCount++ + " of " + messageSize);
            invalidationService.invalidate(invalidationMessage);
        }
    }
}
