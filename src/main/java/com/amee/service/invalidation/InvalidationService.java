package com.amee.service.invalidation;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.IAMEEEntityReference;
import com.amee.messaging.MessageService;
import com.amee.messaging.config.ExchangeConfig;
import com.amee.messaging.config.PublishConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class InvalidationService implements ApplicationContextAware, ApplicationListener {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private MessageService messageService;

    @Autowired
    @Qualifier("invalidationExchange")
    private ExchangeConfig exchangeConfig;

    @Autowired
    @Qualifier("invalidationPublish")
    private PublishConfig publishConfig;

    protected ApplicationContext applicationContext;

    private ThreadLocal<Set<InvalidationMessage>> invalidationMessages = new ThreadLocal<Set<InvalidationMessage>>() {
        protected Set<InvalidationMessage> initialValue() {
            return new HashSet<InvalidationMessage>();
        }
    };

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.trace("onApplicationEvent() BEFORE_BEGIN");
                    onBeforeBegin();
                    break;
                case ROLLBACK:
                    log.trace("onApplicationEvent() ROLLBACK");
                    onRollback();
                    break;
                case END:
                    log.trace("onApplicationEvent() END");
                    onEnd();
                    break;
                default:
                    // Do nothing!
            }
        }
    }

    /**
     * Clears the entities Set. Called before each request is handled.
     * <p/>
     * We do this as the Thread may have been pooled before this execution.
     */
    public synchronized void onBeforeBegin() {
        log.trace("onBeforeBegin()");
        invalidationMessages.get().clear();
    }

    /**
     * Adds an InvalidationMessage to the entities Set for the supplied entity. This will later be sent out
     * on the invalidation topic.
     *
     * @param entity to invalidate caches for
     */
    public synchronized void add(IAMEEEntityReference entity) {
        log.trace("add()");
        invalidationMessages.get().add(new InvalidationMessage(this, entity));
    }

    /**
     * Adds an InvalidationMessage to the entities Set for the supplied entity. This will later be sent out
     * on the invalidation topic.
     *
     * @param entity  to invalidate caches for
     * @param options invalidation options
     */
    public synchronized void add(IAMEEEntityReference entity, String options) {
        log.trace("add()");
        invalidationMessages.get().add(new InvalidationMessage(this, entity, options));
    }

    /**
     * Clears the invalidationMessages set due to transaction rollback.
     */
    public synchronized void onRollback() {
        log.trace("onRollback()");
        invalidationMessages.get().clear();
    }

    /**
     * Triggers entity invalidation. Sends InvalidationMessages into the invalidation topic for
     * the previously added in entities. Called after each request has been handled.
     */
    public synchronized void onEnd() {
        log.trace("onEnd()");
        // Copy the set of InvalidationMessages.
        Set<InvalidationMessage> invalidationMessagesCopy = new HashSet<InvalidationMessage>(invalidationMessages.get());
        // Clear the original set of InvalidationMessages to prevent infinite loop.
        invalidationMessages.get().clear();
        // Now iterate over the copied set.
        for (InvalidationMessage invalidationMessage : invalidationMessagesCopy) {
            invalidate(invalidationMessage);
        }
    }

    /**
     * Invalidate the specified entity.
     *
     * @param entity to publish
     */
    public void invalidate(IAMEEEntityReference entity) {
        invalidate(entity, null);
    }

    /**
     * Invalidate the specified entity.
     *
     * @param entity  to publish
     * @param options invalidation options
     */
    public void invalidate(IAMEEEntityReference entity, String options) {
        InvalidationMessage m = new InvalidationMessage(this, entity, options);
        invalidate(m);
    }

    /**
     * Sends an InvalidationMessage into the invalidation topic and publishes an InvalidationMessage into
     * the local ApplicationContext.
     *
     * @param invalidationMessage to publish
     */
    public void invalidate(InvalidationMessage invalidationMessage) {
        // Invalidate locally.
        applicationContext.publishEvent(invalidationMessage);
        // Publish to queue.
        messageService.publish(
                exchangeConfig,
                publishConfig,
                "platform." + publishConfig.getScope() + ".invalidation",
                invalidationMessage);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
