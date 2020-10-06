package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HandleIotQueryEvent implements HandleIotEvent {
    private static final String NEWEST_MESSAGE_ID_KEY = "newestMessageId";
    private static final String OLDEST_MESSAGE_ID_KEY = "oldestMessageId";
    private static final String NO_MESSAGES_AVAILABLE = "No messages available";

    @Override
    public String getOperationType() {
        return "query";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, String uuid, Optional<String> optionalMessageId, Optional<String> optionalRecipientId) {
        // Get the row with the exact UUID only, no message ID specified
        Map<String, Condition> keyConditions = new HashMap<>();
        AttributeValue uuidAttributeValue = AttributeValue.builder().s(uuid).build();
        Condition uuidCondition = Condition.builder()
                .attributeValueList(uuidAttributeValue)
                .comparisonOperator(ComparisonOperator.EQ)
                .build();
        keyConditions.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuidCondition);

        // Find the oldest row with this UUID
        Optional<String> optionalOldestMessageId = getOldestMessageId(keyConditions);

        // Return a payload on the response topic that contains the UUID
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuid);

        if (optionalOldestMessageId.isPresent()) {
            // There is an oldest message available, add the message ID to the payload
            String oldestMessageId = optionalOldestMessageId.get();
            payloadMap.put(OLDEST_MESSAGE_ID_KEY, oldestMessageId);

            // Newest message ID should always be present but we use ifPresent to be safe in the event that someone deleted the message
            Optional<String> optionalNewestMessageId = getNewestMessageId(keyConditions);
            optionalNewestMessageId.ifPresent(newestMessageId -> payloadMap.put(NEWEST_MESSAGE_ID_KEY, newestMessageId));
        } else {
            // The message was not found, include an error message
            payloadMap.put(SharedHelper.ERROR_KEY, NO_MESSAGES_AVAILABLE);
        }

        publishResponse(uuid, optionalMessageId, Optional.empty(), responseToken, payloadMap);

        return "done";
    }

    @Override
    public boolean isMessageIdRequired() {
        return false;
    }

    @Override
    public boolean isRecipientUuidRequired() {
        return false;
    }
}

