package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface HandleIotDevicesEvent extends HandleIotEvent {
    Logger log = LoggerFactory.getLogger(HandleIotDevicesEvent.class);

    @Override
    default String getOperationType() {
        return "devices";
    }

    @Override
    default String innerHandle(String responseToken, final Map input, String uuid, Optional<String> optionalMessageId, Optional<String> optionalRecipientId) {
        List<String> uuids = getDevices();

        // Put the list of UUIDs in the map
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME + "s", uuids);

        publishResponse(uuid, optionalMessageId, Optional.empty(), responseToken, payloadMap);

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
