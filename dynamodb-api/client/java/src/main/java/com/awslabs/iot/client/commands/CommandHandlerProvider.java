package com.awslabs.iot.client.commands;

import com.awslabs.iot.client.commands.interfaces.CommandHandler;

import java.util.Set;

public interface CommandHandlerProvider {
    Set<CommandHandler> getCommandHandlerSet();
}
