package com.amee.service.invalidation;

import com.amee.domain.AMEEEntityReference;
import com.amee.domain.ObjectType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;

public class Invalidator {

    public final static Invalidator INSTANCE = new Invalidator();

    public static void main(String[] args) {
        try {
            if (args.length == 7) {
                INSTANCE.sendMessage(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                System.out.println("Not enough parameters.");
            }
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e);
        }
    }

    public void sendMessage(
            String scope,
            String hostName,
            String portNumber,
            String username,
            String password,
            String entityType,
            String entityUid) throws IOException {
        // Create the InvalidationMessage.
        InvalidationMessage message =
                new InvalidationMessage(
                        this,
                        new AMEEEntityReference(ObjectType.valueOf(entityType), entityUid));
        // Configure RabbitMQ.
        ConnectionParameters connectionParameters = new ConnectionParameters();
        connectionParameters.setUsername(username);
        connectionParameters.setPassword(password);
        ConnectionFactory connectionFactory = new ConnectionFactory(connectionParameters);
        Connection connection = connectionFactory.newConnection(hostName, Integer.valueOf(portNumber));
        Channel channel = connection.createChannel();
        // Send the message.
        channel.basicPublish(
                "platform." + scope + ".invalidation",
                "platform." + scope + ".invalidation",
                false,
                false,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                message.getMessage().getBytes());
        // We're done with the channel.
        channel.close();
    }
}
