package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;

import java.util.Map;

public class HandleIotNextEvent implements HandleIotEvent {
    private static final String SPECIFIED_MESSAGE_ID_KEY = "specifiedMessageId";
    private static final String NEXT_MESSAGE_ID_KEY = "nextMessageId";
    private static final String NO_MESSAGES_AVAILABLE = "No newer messages available";

    @Override
    public String getOperationType() {
        return "next";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, Option<String> uuidOption, Option<String> messageIdOption, Option<String> recipientIdOption) {
        String messageId = messageIdOption.get();
        String uuid = uuidOption.get();

        // Get the row with the exact UUID and the next message ID (message ID greater than the value specified in the request)

        AttributeValue uuidAttributeValue = AttributeValue.builder().s(uuid).build();
        Condition uuidCondition = Condition.builder()
                .attributeValueList(uuidAttributeValue)
                .comparisonOperator(ComparisonOperator.EQ)
                .build();
        HashMap<String, Condition> keyConditions = HashMap.of(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuidCondition);

        AttributeValue messageIdAttributeValue = AttributeValue.builder().s(messageId).build();
        Condition messageIdCondition = Condition.builder()
                .attributeValueList(messageIdAttributeValue)
                .comparisonOperator(ComparisonOperator.GT)
                .build();
        keyConditions = keyConditions.put(SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, messageIdCondition);

        Option<String> oldestMessageIdOption = getOldestMessageId(keyConditions);

        // Return a payload on the response topic that contains the UUID and specified message ID
        HashMap<String, String> payloadMap = HashMap.of(
                SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuid,
                SPECIFIED_MESSAGE_ID_KEY, messageId);

        if (oldestMessageIdOption.isDefined()) {
            // There is a next message available, add the message ID to the payload
            String oldestMessageId = oldestMessageIdOption.get();
            payloadMap = payloadMap.put(NEXT_MESSAGE_ID_KEY, oldestMessageId);
        } else {
            // The message was not found, include an error message
            payloadMap = payloadMap.put(SharedHelper.ERROR_KEY, NO_MESSAGES_AVAILABLE);
        }

        publishResponse(uuidOption, messageIdOption, Option.none(), responseToken, payloadMap);

        return "done";
    }

    @Override
    public boolean isMessageIdRequired() {
        // Yes, we need a message ID for a next message request
        return true;
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
