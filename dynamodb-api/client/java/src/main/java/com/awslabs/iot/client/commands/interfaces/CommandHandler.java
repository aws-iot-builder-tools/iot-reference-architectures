package com.awslabs.iot.client.commands.interfaces;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import io.vavr.control.Try;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public interface CommandHandler {
    boolean supportsSerial();

    boolean supportsMqtt();

    /**
     * Handles the command specified in the input, returns true if the command was acted on by this handler.  Returns false otherwise.
     *
     * @param input
     * @return
     */
    default boolean handle(String input) {
        if (!isHandled(input)) {
            return false;
        }

        if (!parametersSpecified(input)) {
            showUsage(null);
            return true;
        }

        innerHandle(input);

        return true;
    }

    /**
     * Gets the string that starts this command
     *
     * @return
     */
    default String getFullCommandString() {
        String serviceName = getServiceName();

        if (serviceName != null) {
            // Prefix service specific commands with their service name
            return String.join("", serviceName, "-", getCommandString());
        }

        return getCommandString();
    }

    /**
     * Returns the help for this command
     *
     * @return
     */
    String getHelp();

    default Completer getCommandNameCompleter() {
        return new StringsCompleter(getFullCommandString());
    }

    default Completer getCompleter() {
        return new ArgumentCompleter(getCommandNameCompleter(), new NullCompleter());
    }

    void innerHandle(String input);

    default boolean isHandled(String input) {
        return Try.of(() -> {
            String commandName = input.split(" ")[0];

            return getFullCommandString().equals(commandName);
        })
                .recover(Exception.class, throwable -> false)
                .get();
    }

    default String getServiceName() {
        return null;
    }

    String getCommandString();

    int requiredParameters();

    default Optional<Integer> maximumParameters() {
        return Optional.empty();
    }

    default boolean parametersSpecified(String input) {
        List<String> parameters = getParameterExtractor().getParameters(input);
        int parameterCount = parameters.size();
        return (requiredParameters() == parameterCount) ||
                ((maximumParameters().isPresent()) && (parameterCount >= requiredParameters()));
    }

    ParameterExtractor getParameterExtractor();

    default void showUsage(Logger logger) {
        if (logger == null) {
            logger = LoggerFactory.getLogger(CommandHandler.class);
        }

        logger.info(String.join("", "No usage information has been provided for this command, but the required number of parameters were not specified.  Expected ", String.valueOf(requiredParameters()), " parameter(s)."));
    }

    IoHelper getIoHelper();

    /**
     * Override this to return true for functions that are too dangerous to let the user use unless they specify a special option
     *
     * @return
     */
    default boolean isDangerous() {
        return false;
    }
}
