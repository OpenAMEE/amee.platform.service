package com.amee.service.invalidation;

import com.amee.messaging.TopicMessageConsumer;
import com.amee.messaging.config.ConsumeConfig;
import com.amee.messaging.config.ExchangeConfig;
import com.amee.messaging.config.QueueConfig;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class InvalidationMessageConsumer extends TopicMessageConsumer {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    @Qualifier("invalidationExchange")
    private ExchangeConfig exchangeConfig;

    @Autowired
    @Qualifier("invalidationQueue")
    private QueueConfig queueConfig;

    @Autowired
    @Qualifier("invalidationConsume")
    private ConsumeConfig consumeConfig;

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