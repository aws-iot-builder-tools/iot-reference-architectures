package com.awslabs.iot.client.commands.mqtt;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.commands.SharedCommunication;
import com.awslabs.iot.client.data.NextResponse;
import com.awslabs.iot.client.commands.SharedMqtt;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class NextCommandHandler implements CommandHandler {
    private final Logger log = LoggerFactory.getLogger(NextCommandHandler.class);
    private static final String NEXT = SharedCommunication.NEXT;
    @Inject
    ParameterExtractor parameterExtractor;
    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SharedMqtt sharedMqtt;

    @Inject
    public NextCommandHandler() {
    }

    @Override
    public int requiredParameters() {
        return 1;
    }

    @Override
    public void innerHandle(String input) {
        List<String> parameters = parameterExtractor.getParameters(input);

        String messageId = parameters.get(0);

        Optional<NextResponse> optionalNextResponse = sharedMqtt.nextMessage(messageId);

        if (!optionalNextResponse.isPresent()) {
            log.error("No next response received");
            return;
        }

        log.info(jsonHelper.toJson(optionalNextResponse.get()));
    }

    @Override
    public String getCommandString() {
        return NEXT;
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
        return "Gets the next message ID after a specified message ID.";
    }

    public ParameterExtractor getParameterExtractor() {
        return this.parameterExtractor;
    }

    public IoHelper getIoHelper() {
        return this.ioHelper;
    }
}
