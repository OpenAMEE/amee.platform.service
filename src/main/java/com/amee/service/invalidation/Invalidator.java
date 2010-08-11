package com.amee.service.invalidation;

import com.amee.domain.AMEEEntityReference;
import com.amee.domain.ObjectType;
import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.io.IOException;

public class Invalidator {

    public final static Invalidator INSTANCE = new Invalidator();

    public static void main(String[] args) {
        System.out.println("Starting...");
        try {
            if (args.length == 7) {
                INSTANCE.sendMessage(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                System.out.println("Not enough parameters.");
            }
        } catch (IOException e) {
            if (e.getCause() == null) {
                System.out.println("Caught IOException: " + e.getMessage());
            } else {
                System.out.println("Caught IOException: " + e.getMessage() + " Cause: " + e.getCause().getMessage());
            }
        }
        System.out.println("...done.");
    }

    public void sendMessage(
            String scope,
            String hostName,
            String portNumber,
            String username,
            String password,
            String entityType,
            String entityUid) throws IOException {
        System.out.println("    1) Create the InvalidationMessage.");
        InvalidationMessage message =
                new InvalidationMessage(
                        this,
                        new AMEEEntityReference(ObjectType.valueOf(entityType), entityUid));
        message.setOptions("indexDataItems");
        System.out.println("    2) Configure RabbitMQ.");
        ConnectionParameters connectionParameters = new ConnectionParameters();
        connectionParameters.setUsername(username);
        connectionParameters.setPassword(password);
        ConnectionFactory connectionFactory = new ConnectionFactory(connectionParameters);
        Connection connection = connectionFactory.newConnection(hostName, Integer.valueOf(portNumber));
        Channel channel = connection.createChannel();
        System.out.println("    3) Send the message.");
        String messageText = message.getMessage();
        System.out.println("        Message: " + messageText);
        System.out.println("        Destination: platform." + scope + ".invalidation");
        channel.basicPublish(
                "platform." + scope + ".invalidation",
                "platform." + scope + ".invalidation",
                false,
                false,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                messageText.getBytes());
        System.out.println("    4) Close the channel & connection.");
        channel.close();
        connection.close();
    }
}
