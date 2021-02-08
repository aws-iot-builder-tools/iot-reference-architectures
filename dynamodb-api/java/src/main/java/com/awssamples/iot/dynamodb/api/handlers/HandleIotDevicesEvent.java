package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public interface HandleIotDevicesEvent extends HandleIotEvent {
    Logger log = LoggerFactory.getLogger(HandleIotDevicesEvent.class);

    @Override
    default String getOperationType() {
        return "devices";
    }

    @Override
    default String innerHandle(String responseToken, final Map input, Option<String> uuidOption, Option<String> messageIdOption, Option<String> recipientIdOption) {
        // Put the list of UUIDs in the map
        HashMap<String, Object> payloadMap = HashMap.of(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME + "s", getDevices());

        publishResponse(Option.none(), Option.none(), Option.none(), responseToken, payloadMap);

        return "done";
    }

    default boolean isMessageIdRequired() {
        // No message ID required
        return false;
    }

    default boolean isRecipientUuidRequired() {
        return false;
    }

    List<String> getDevices();
}
