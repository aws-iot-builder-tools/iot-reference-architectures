package com.awslabs.iot.client.commands.mqtt;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.commands.SharedMqtt;
import com.awslabs.iot.client.data.NextResponse;
import com.awslabs.iot.client.data.QueryResponse;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class GetAllCommandHandler implements CommandHandler {
    private final Logger log = LoggerFactory.getLogger(GetAllCommandHandler.class);
    private static final String GET = "get-all";
    @Inject
    ParameterExtractor parameterExtractor;
    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SharedMqtt sharedMqtt;

    @Inject
    public GetAllCommandHandler() {
    }

    @Override
    public int requiredParameters() {
        return 0;
    }

    @Override
    public void innerHandle(String input) {
        Optional<QueryResponse> optionalQueryResponse = sharedMqtt.query();

        if (!optionalQueryResponse.isPresent()) {
            log.error("No query response received");
            return;
        }

        QueryResponse queryResponse = optionalQueryResponse.get();

        if (queryResponse.getError().isPresent()) {
            log.error(queryResponse.getError().get());
            return;
        }

        String messageId = queryResponse.getOldestMessageId().get();

        while (messageId != null) {
            sharedMqtt.getAndDisplayMessage(messageId);

            Optional<NextResponse> optionalNextResponse = sharedMqtt.nextMessage(messageId);

            if (!optionalNextResponse.isPresent()) {
                log.error("No next response received");
                return;
            }

            NextResponse nextResponse = optionalNextResponse.get();

            if (nextResponse.getError().isPresent()) {
                log.warn(nextResponse.getError().get());
                messageId = null;
            } else if (nextResponse.getNextMessageId().isPresent()) {
                messageId = nextResponse.getNextMessageId().get();
            } else {
                log.error("No error and no next message ID. This is a bug. Cannot continue.");
                System.exit(1);
            }
        }
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
        return "Gets all available messages.";
    }

    public ParameterExtractor getParameterExtractor() {
        return this.parameterExtractor;
    }

    public IoHelper getIoHelper() {
        return this.ioHelper;
    }
}
