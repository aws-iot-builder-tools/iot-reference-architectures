package com.awslabs.iot.client.commands.mqtt;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.commands.SharedCommunication;
import com.awslabs.iot.client.commands.SharedMqtt;
import com.awslabs.iot.client.data.QueryResponse;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class QueryCommandHandler implements CommandHandler {
    private final Logger log = LoggerFactory.getLogger(QueryCommandHandler.class);
    private static final String QUERY = SharedCommunication.QUERY;
    @Inject
    ParameterExtractor parameterExtractor;
    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SharedMqtt sharedMqtt;

    @Inject
    public QueryCommandHandler() {
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

        log.info(jsonHelper.toJson(optionalQueryResponse.get()));
    }

    @Override
    public String getCommandString() {
        return QUERY;
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
        return "Gets the available message info.";
    }

    public ParameterExtractor getParameterExtractor() {
        return this.parameterExtractor;
    }

    public IoHelper getIoHelper() {
        return this.ioHelper;
    }
}
