package com.awslabs.iot.client.interfaces;

import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import io.vavr.control.Try;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public interface DynamoDbApiClientTerminal {
    String BLANK_STRING = "";

    default String getPrompt() {
        return String.join("", getPromptColor(), "> ", getTextColor());
    }

    String getTextColor();

    String getPromptColor();

    default void mainLoop() {
        Set<String> fullCommands = new HashSet<>();

        Set<CommandHandler> commandHandlerSet = getCommandHandlerSet();

        for (CommandHandler commandHandler : commandHandlerSet) {
            int previousSize = fullCommands.size();
            String fullCommandString = commandHandler.getFullCommandString();
            fullCommands.add(fullCommandString);

            if (fullCommands.size() == previousSize) {
                String message = String.join("", "Duplicate command string found [", fullCommandString, "]");
                log(message);
                throw new UnsupportedOperationException(message);
            }
        }

        DefaultHistory history = new DefaultHistory();

        Terminal terminal = Try.of(this::getTerminal).get();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(getCommandsCompleter())
                .history(history)
                .build();

        Try.of(() -> infiniteInputLoop(reader))
                .recover(UserInterruptException.class, this::handleUserInterruptException)
                .get();
    }

    default Void infiniteInputLoop(LineReader reader) {
        while (true) {
            String command = reader.readLine(getPrompt());

            if (BLANK_STRING.equals(command)) {
                continue;
            }

            boolean handled = false;
            command = command.trim();

            for (CommandHandler commandHandler : getCommandHandlerSet()) {
                if (commandHandler.handle(command)) {
                    handled = true;
                    reader.getTerminal().flush();
                    break;
                }
            }

            if (!handled) {
                log(String.join("", "The command [", command, "] was not understood."));
            }
        }
    }

    default Void handleUserInterruptException(UserInterruptException userInterruptException) {
        // User probably hit CTRL-C, just bail out
        log("User may have hit CTRL-C, exiting");
        System.exit(1);

        return null;
    }

    Terminal getTerminal() throws IOException;

    Completer getCommandsCompleter();

    Set<CommandHandler> getCommandHandlerSet();

    void log(String s);

    void start();
}