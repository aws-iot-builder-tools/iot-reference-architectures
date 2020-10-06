package com.awslabs.iot.client.applications;

import com.awslabs.iot.client.interfaces.DynamoDbApiClientTerminal;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DynamoDbApiClientConsole {
    private static final Logger log = LoggerFactory.getLogger(DynamoDbApiClientConsole.class);

    public static void main(String[] args) throws Exception {
        Injector injector = DaggerInjector.create();
        injector.DynamoDbApiClientConsole().run(args);
    }

    @Inject
    public DynamoDbApiClientConsole() {
    }

    public void run(String[] args) {
        Arguments arguments = new Arguments();

        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);

        Injector injector = DaggerInjector.create();

        if ((!arguments.serial) && (arguments.uuid == null)) {
            // If the user isn't using a real SBD device then they must specify their UUID
            log.error("UUID is a required argument");
            System.exit(1);
        }

        // Set global arguments
        injector.arguments().uuid = arguments.uuid;
        injector.arguments().serial = arguments.serial;

        DynamoDbApiClientTerminal dynamoDbApiClientTerminal = injector.DynamoDbApiClientTerminal();
        dynamoDbApiClientTerminal.start();
    }
}
