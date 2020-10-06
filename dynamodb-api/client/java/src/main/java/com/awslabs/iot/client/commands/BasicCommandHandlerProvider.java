package com.awslabs.iot.client.commands;

import com.awslabs.iot.client.applications.Arguments;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BasicCommandHandlerProvider implements CommandHandlerProvider {
    @Inject
    Arguments arguments;

    @Inject
    Set<CommandHandler> commandHandlerSet;

    @Inject
    public BasicCommandHandlerProvider() {
    }

    @Override
    public Set<CommandHandler> getCommandHandlerSet() {
        Stream<CommandHandler> commandHandlerStream = commandHandlerSet.stream();

        if (arguments.serial) {
            // Only return supported serial port commands
            commandHandlerStream = commandHandlerStream.filter(CommandHandler::supportsSerial);
        } else {
            // Only return supported MQTT commands
            commandHandlerStream = commandHandlerStream.filter(CommandHandler::supportsMqtt);
        }

        return commandHandlerStream.collect(Collectors.toSet());
    }
}
