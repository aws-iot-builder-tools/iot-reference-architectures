package com.awslabs.iot.client.commands.mqtt;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.commands.SharedCommunication;
import com.awslabs.iot.client.commands.SharedMqtt;
import com.awslabs.iot.client.data.DeleteResponse;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class DeleteCommandHandler implements CommandHandler {
    private final Logger log = LoggerFactory.getLogger(DeleteCommandHandler.class);
    private static final String DELETE = SharedCommunication.DELETE;
    @Inject
    ParameterExtractor parameterExtractor;
    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SharedMqtt sharedMqtt;

    @Inject
    public DeleteCommandHandler() {
    }

    @Override
    public int requiredParameters() {
        return 1;
    }

    @Override
    public void innerHandle(String input) {
        List<String> parameters = parameterExtractor.getParameters(input);

        String messageId = parameters.get(0);

        Optional<DeleteResponse> optionalDeleteResponse = sharedMqtt.deleteMessage(messageId);

        if (!optionalDeleteResponse.isPresent()) {
            log.error("No delete response received");
            return;
        }

        log.info(jsonHelper.toJson(optionalDeleteResponse.get()));
    }

    @Override
    public String getCommandString() {
        return DELETE;
    }

    @Override
    public boolean supportsSerial() {
        return false;
    }

    @Override
    public boolean supportsMqtt() {
        return true;
    }

    @Override
    public String getHelp() {
        return "Deletes the specified message ID.";
    }

    public ParameterExtractor getParameterExtractor() {
        return this.parameterExtractor;
    }

    public IoHelper getIoHelper() {
        return this.ioHelper;
    }
}
