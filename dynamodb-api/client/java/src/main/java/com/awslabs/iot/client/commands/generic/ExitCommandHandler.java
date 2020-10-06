package com.awslabs.iot.client.commands.generic;

import javax.inject.Inject;

public class ExitCommandHandler extends QuitCommandHandler {
    private static final String EXIT = "exit";

    @Inject
    public ExitCommandHandler() {
    }

    @Override
    public String getCommandString() {
        return EXIT;
    }
}
