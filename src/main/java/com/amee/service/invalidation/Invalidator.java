package com.amee.service.invalidation;

import com.amee.domain.AMEEEntityReference;
import com.amee.domain.ObjectType;
import com.rabbitmq.client.*;
import org.apache.commons.cli.*;

import java.io.IOException;

public class Invalidator {

    public final static Invalidator INSTANCE = new Invalidator();

    private Invalidator() {
        super();
    }

    public static void main(String[] args) {
        try {
            INSTANCE.sendMessage(args);
        } catch (IOException e) {
            if (e.getCause() == null) {
                System.out.println("Caught IOException: " + e.getMessage());
            } else {
                System.out.println("Caught IOException: " + e.getMessage() + " Cause: " + e.getCause().getMessage());
            }
        }
    }

    private void sendMessage(String[] args) throws IOException {

        String scope = null;
        String host = "localhost";
        String port = "5672";
        String username = null;
        String password = null;
        String entityType = "DC";
        String entityUid = null;
        String invOptions = null;

        CommandLine line = null;
        CommandLineParser parser = new GnuParser();
        Options options = new Options();

        // Define scope option.
        Option scopeOpt = OptionBuilder
                .withArgName("scope")
                .hasArg()
                .isRequired()
                .withDescription("The messaging scope name (e.g., 'live', 'science' or 'stage'). Required")
                .create("scope");
        options.addOption(scopeOpt);

        // Define host option.
        Option hostOpt = OptionBuilder
                .withArgName("host")
                .hasArg()
                .withDescription("The RabbitMQ host name. Optional, defaults to 'localhost'.")
                .create("host");
        options.addOption(hostOpt);

        // Define port option.
        Option portOpt = OptionBuilder
                .withArgName("port")
                .hasArg()
                .withDescription("The RabbitMQ port number. Optional, defaults to '5672'.")
                .create("port");
        options.addOption(portOpt);

        // Define username option.
        Option usernameOpt = OptionBuilder
                .withArgName("username")
                .hasArg()
                .isRequired()
                .withDescription("The RabbitMQ username. Required.")
                .create("username");
        options.addOption(usernameOpt);

        // Define password option.
        Option passwordOpt = OptionBuilder
                .withArgName("password")
                .hasArg()
                .isRequired()
                .withDescription("The RabbitMQ password. Required.")
                .create("password");
        options.addOption(passwordOpt);

        // Define entityType option.
        Option entityTypeOpt = OptionBuilder
                .withArgName("entityType")
                .hasArg()
                .withDescription("The entity type to invalidate. Optional, defaults to 'DC'.")
                .create("entityType");
        options.addOption(entityTypeOpt);

        // Define entityUids option.
        Option entityUidsOpt = OptionBuilder
                .withArgName("entityUid")
                .hasArg()
                .isRequired()
                .withDescription("The entity UIDs to invalidate. This can be a single UID or a CSV list (e.g., 'AAAAAAAAAAAA,BBBBBBBBBBBB'). Required.")
                .create("entityUids");
        options.addOption(entityUidsOpt);

        // Define entityUid option.
        Option invOptionsOpt = OptionBuilder
                .withArgName("invOptions")
                .hasArg()
                .isRequired()
                .withDescription("Extra options for the invalidation message. This can be a single option or a CSV list (e.g., 'indexDataItems'). Optional.")
                .create("invOptions");
        options.addOption(invOptionsOpt);

        // Parse the options.
        try {
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            new HelpFormatter().printHelp("java " + this.getClass().getName(), options);
            System.exit(-1);
        }

        // Handle scope.
        if (line.hasOption(scopeOpt.getOpt())) {
            scope = line.getOptionValue(scopeOpt.getOpt());
        }

        // Handle host.
        if (line.hasOption(hostOpt.getOpt())) {
            host = line.getOptionValue(hostOpt.getOpt());
        }

        // Handle port.
        if (line.hasOption(portOpt.getOpt())) {
            port = line.getOptionValue(portOpt.getOpt());
        }

        // Handle username.
        if (line.hasOption(usernameOpt.getOpt())) {
            username = line.getOptionValue(usernameOpt.getOpt());
        }

        // Handle password.
        if (line.hasOption(passwordOpt.getOpt())) {
            password = line.getOptionValue(passwordOpt.getOpt());
        }

        // Handle entityType.
        if (line.hasOption(entityTypeOpt.getOpt())) {
            entityType = line.getOptionValue(entityTypeOpt.getOpt());
        }

        // Handle entityUid.
        if (line.hasOption(entityUidsOpt.getOpt())) {
            entityUid = line.getOptionValue(entityUidsOpt.getOpt());
        }

        // Handle invOptions.
        if (line.hasOption(invOptionsOpt.getOpt())) {
            invOptions = line.getOptionValue(invOptionsOpt.getOpt());
        }

        // Send the message!
        sendMessage(scope, host, port, username, password, entityType, entityUid, invOptions);
    }

    private void sendMessage(
            String scope,
            String hostName,
            String portNumber,
            String username,
            String password,
            String entityType,
            String entityUids,
            String invOptions) throws IOException {
        System.out.println("    Configure RabbitMQ.");
        ConnectionParameters connectionParameters = new ConnectionParameters();
        connectionParameters.setUsername(username);
        connectionParameters.setPassword(password);
        ConnectionFactory connectionFactory = new ConnectionFactory(connectionParameters);
        Connection connection = connectionFactory.newConnection(hostName, Integer.valueOf(portNumber));
        Channel channel = connection.createChannel();
        System.out.println("    Send the messages.");
        for (String entityUid : entityUids.split(",")) {
            System.out.println("    Create InvalidationMessages.");
            InvalidationMessage message =
                    new InvalidationMessage(
                            this,
                            new AMEEEntityReference(ObjectType.valueOf(entityType), entityUid),
                            invOptions);
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
        }
        System.out.println("    Close the channel & connection.");
        channel.close();
        connection.close();
    }
}
