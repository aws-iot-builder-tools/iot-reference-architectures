package com.awslabs.iot.client.commands.mqtt;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.commands.SharedCommunication;
import com.awslabs.iot.client.commands.SharedMqtt;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class GetCommandHandler implements CommandHandler {
    private final Logger log = LoggerFactory.getLogger(GetCommandHandler.class);
    private static final String GET = SharedCommunication.GET;
    @Inject
    ParameterExtractor parameterExtractor;
    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SharedMqtt sharedMqtt;

    @Inject
    public GetCommandHandler() {
    }

    @Override
    public int requiredParameters() {
        return 1;
    }

    @Override
    public void innerHandle(String input) {
        List<String> parameters = parameterExtractor.getParameters(input);

        String messageId = parameters.get(0);

        sharedMqtt.getAndDisplayMessage(messageId);
    }

    @Override
    public String getCommandString() {
        return GET;
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
        return "Gets a message.";
    }

    public ParameterExtractor getParameterExtractor() {
        return this.parameterExtractor;
    }

    public IoHelper getIoHelper() {
        return this.ioHelper;
    }
}
