package com.amee.service.invalidation;

import com.amee.base.transaction.TransactionEvent;
import com.amee.domain.AMEEEntityReference;
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

    private ThreadLocal<Set<IAMEEEntityReference>> entities = new ThreadLocal<Set<IAMEEEntityReference>>() {
        protected Set<IAMEEEntityReference> initialValue() {
            return new HashSet<IAMEEEntityReference>();
        }
    };

    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent) e;
            switch (te.getType()) {
                case BEFORE_BEGIN:
                    log.debug("onApplicationEvent() BEFORE_BEGIN");
                    onBeforeBegin();
                    break;
                case END:
                    log.debug("onApplicationEvent() END");
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
        log.debug("onBeforeBegin()");
        entities.get().clear();
    }

    /**
     * Adds an InvalidationMessage to the entities Set for the supplied entity. This will later be sent out
     * on the invalidation topic.
     *
     * @param entity to invalidate caches for
     */
    public synchronized void add(IAMEEEntityReference entity) {
        log.debug("add()");
        // Add entity to list of entities to invalidate.
        entities.get().add(new AMEEEntityReference(entity));
    }

    /**
     * Triggers entity invalidation. Sends InvalidationMessages into the invalidation topic for
     * the previously added in entities. Called after each request has been handled.
     */
    public synchronized void onEnd() {
        log.debug("onEnd()");
        try {
            for (IAMEEEntityReference entity : entities.get()) {
                invalidate(entity);
            }
        } finally {
            entities.get().clear();
        }
    }

    /**
     * Invalidate the specified entity from the cache. Sends an InvalidationMessage into the invalidation
     * topic for the specified entity and publishes an InvalidationMessage into the local ApplicationContext.
     *
     * @param entity to publish
     */
    public void invalidate(IAMEEEntityReference entity) {
        InvalidationMessage m = new InvalidationMessage(this, entity);
        // Invalidate locally.
        applicationContext.publishEvent(m);
        // Publish to queue.
        messageService.publish(
                exchangeConfig,
                publishConfig,
                "platform." + publishConfig.getScope() + ".invalidation",
                m);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
