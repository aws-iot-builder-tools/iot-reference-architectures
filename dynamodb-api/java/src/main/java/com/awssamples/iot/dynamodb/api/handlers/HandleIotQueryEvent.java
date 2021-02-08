package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;

import java.util.Map;

public class HandleIotQueryEvent implements HandleIotEvent {
    private static final String NEWEST_MESSAGE_ID_KEY = "newestMessageId";
    private static final String OLDEST_MESSAGE_ID_KEY = "oldestMessageId";
    private static final String NO_MESSAGES_AVAILABLE = "No messages available";

    @Override
    public String getOperationType() {
        return "query";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, Option<String> uuidOption, Option<String> messageIdOption, Option<String> recipientIdOption) {
        String uuid = uuidOption.get();

        // Get the row with the exact UUID only, no message ID specified
        AttributeValue uuidAttributeValue = AttributeValue.builder().s(uuid).build();
        Condition uuidCondition = Condition.builder()
                .attributeValueList(uuidAttributeValue)
                .comparisonOperator(ComparisonOperator.EQ)
                .build();
        HashMap<String, Condition> keyConditions = HashMap.of(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuidCondition);

        // Find the oldest row with this UUID
        Option<String> oldestMessageIdOption = getOldestMessageId(keyConditions);

        // Return a payload on the response topic that contains the UUID
        HashMap<String, String> payloadMap = HashMap.of(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuid);

        if (oldestMessageIdOption.isDefined()) {
            // There is an oldest message available, add the message ID to the payload
            String oldestMessageId = oldestMessageIdOption.get();
            payloadMap = payloadMap.put(OLDEST_MESSAGE_ID_KEY, oldestMessageId);

            // Newest message ID should always be present but we use ifPresent to be safe in the event that someone deleted the message
            Option<String> newestMessageIdOption = getNewestMessageId(keyConditions);

            if (newestMessageIdOption.isDefined()) {
                payloadMap = payloadMap.put(NEWEST_MESSAGE_ID_KEY, newestMessageIdOption.get());
            }
        } else {
            // The message was not found, include an error message
            payloadMap = payloadMap.put(SharedHelper.ERROR_KEY, NO_MESSAGES_AVAILABLE);
        }

        publishResponse(uuidOption, Option.none(), Option.none(), responseToken, payloadMap);

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

    @Override
    public boolean isDeviceUuidRequired() {
        return true;
    }
}

