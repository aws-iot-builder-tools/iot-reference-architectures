package com.awslabs.iot.client.commands.generic;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.iot.client.commands.CommandHandlerProvider;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.helpers.ANSIHelper;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HelpCommandHandler implements CommandHandler {
    private static final String HELP = "help";
    private static final Logger log = LoggerFactory.getLogger(HelpCommandHandler.class);
    @Inject
    Provider<CommandHandlerProvider> commandHandlerProviderProvider;
    @Inject
    IoHelper ioHelper;
    @Inject
    ParameterExtractor parameterExtractor;

    @Inject
    public HelpCommandHandler() {
    }

    @Override
    public void innerHandle(String input) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(ANSIHelper.CRLF);

        List<CommandHandler> sortedCommandHandlers = commandHandlerProviderProvider.get()
                .getCommandHandlerSet().stream()
                .sorted(Comparator.comparing(CommandHandler::getFullCommandString))
                .collect(Collectors.toList());

        for (CommandHandler commandHandler : sortedCommandHandlers) {
            stringBuilder.append(ANSIHelper.BOLD);
            stringBuilder.append(commandHandler.getFullCommandString());
            stringBuilder.append(ANSIHelper.NORMAL);
            stringBuilder.append(" - ");
            stringBuilder.append(commandHandler.getHelp());
            stringBuilder.append(ANSIHelper.CRLF);
        }

        log.info(stringBuilder.toString());
    }

    @Override
    public String getCommandString() {
        return HELP;
    }

    @Override
    public boolean supportsSerial() {
        return true;
    }

    @Override
    public boolean supportsMqtt() {
        return true;
    }

    @Override
    public String getHelp() {
        return "Gets this help message";
    }

    @Override
    public int requiredParameters() {
        return 0;
    }

    public IoHelper getIoHelper() {
        return this.ioHelper;
    }

    public ParameterExtractor getParameterExtractor() {
        return this.parameterExtractor;
    }
}
