package com.amee.service.invalidation;

import com.amee.messaging.TopicMessageConsumer;
import com.amee.messaging.config.ConsumeConfig;
import com.amee.messaging.config.ExchangeConfig;
import com.amee.messaging.config.QueueConfig;
import com.rabbitmq.client.QueueingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Consumes incoming invalidation messages from RabbitMQ and publishes as an {@link InvalidationMessage} into
 * the Spring container.
 */
public class InvalidationMessageConsumer extends TopicMessageConsumer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("invalidationExchange")
    private ExchangeConfig exchangeConfig;

    @Autowired
    @Qualifier("invalidationQueue")
    private QueueConfig queueConfig;

    @Autowired
    @Qualifier("invalidationConsume")
    private ConsumeConfig consumeConfig;

    /**
     * Publish the incoming invalidation message as an InvalidationMessage.
     *
     * @param delivery the incoming message
     */
    public void handle(QueueingConsumer.Delivery delivery) {
        log.debug("handleDelivery()");
        applicationContext.publishEvent(new InvalidationMessage(this, new String(delivery.getBody())));
    }

    public ExchangeConfig getExchangeConfig() {
        return exchangeConfig;
    }

    public QueueConfig getQueueConfig() {
        return queueConfig;
    }

    public ConsumeConfig getConsumeConfig() {
        return consumeConfig;
    }

    public String getBindingKey() {
        return "platform." + consumeConfig.getScope() + ".invalidation";
    }
}
