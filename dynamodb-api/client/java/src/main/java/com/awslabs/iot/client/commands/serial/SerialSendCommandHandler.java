package com.awslabs.iot.client.commands.serial;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.commands.SharedCommunication;
import com.awslabs.iot.client.commands.SharedSerial;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class SerialSendCommandHandler implements CommandHandler {
    private final Logger log = LoggerFactory.getLogger(SerialSendCommandHandler.class);
    private static final String SEND = SharedCommunication.SEND;
    @Inject
    ParameterExtractor parameterExtractor;
    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SharedSerial sharedSerial;

    @Inject
    public SerialSendCommandHandler() {
    }

    @Override
    public int requiredParameters() {
        return 2;
    }

    @Override
    public void innerHandle(String input) {
        List<String> parameters = parameterExtractor.getParameters(input);

        String recipientUuid = parameters.get(0);
        String message = parameters.get(1);

        if (sharedSerial.sendMessage(recipientUuid, message).isPresent()) {
            log.info("Message queued");
        }
    }

    @Override
    public String getCommandString() {
        return SEND;
    }

    @Override
    public boolean supportsSerial() {
        return true;
    }

    @Override
    public boolean supportsMqtt() {
        return false;
    }

    @Override
    public String getHelp() {
        return "Sends a message.";
    }

    public ParameterExtractor getParameterExtractor() {
        return this.parameterExtractor;
    }

    public IoHelper getIoHelper() {
        return this.ioHelper;
    }
}
