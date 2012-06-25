package com.amee.service.invalidation;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.IAMEEEntityReference;
import com.amee.messaging.MessageService;
import com.amee.messaging.config.ExchangeConfig;
import com.amee.messaging.config.PublishConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class InvalidationService implements ApplicationContextAware, ApplicationListener<TransactionEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("invalidationTaskExecutor")
    private TaskExecutor taskExecutor;

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

    public void onApplicationEvent(TransactionEvent te) {
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
        final Set<InvalidationMessage> invalidationMessagesCopy = new HashSet<InvalidationMessage>(invalidationMessages.get());

        // Clear the original set of InvalidationMessages to prevent infinite loop.
        invalidationMessages.get().clear();

        // Invalidate the copied set in a separate thread to prevent large invalidation sets blocking
        // the client response. The invalidate method publishes events to the application context which
        // will be handled synchronously in this new thread.
        taskExecutor.execute(new Runnable() {
            public void run() {
                int messageCount = 0;
                int messageSize = invalidationMessagesCopy.size();
                for (InvalidationMessage invalidationMessage : invalidationMessagesCopy) {
                    log.trace("Invalidating message " + ++messageCount + " of " + messageSize);
                    invalidate(invalidationMessage);
                }
            }
        });
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
