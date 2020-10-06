package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class HandleIotGetEvent implements HandleIotEvent {
    private static final String MESSAGE_NOT_AVAILABLE = "Message not available";

    @Override
    public String getOperationType() {
        return "get";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, String uuid, Optional<String> optionalMessageId, Optional<String> optionalRecipientId) {
        String messageId = optionalMessageId.get();

        // Get the row with the exact UUID and message ID values
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(uuid).build());
        key.put(SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(messageId).build());

        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(SharedHelper.getTableName())
                .key(key)
                .build();
        GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);

        // Return a payload on the response topic that contains the UUID and message ID
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuid);
        payloadMap.put(SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, messageId);

        Map<String, AttributeValue> item = getItemResponse.item();

        if (item.isEmpty()) {
            // The message was not found, include an error message
            payloadMap.put(SharedHelper.ERROR_KEY, MESSAGE_NOT_AVAILABLE);
        } else {
            // The message was found, include the body converted to a normal value from a DynamoDB AttributeValue
            payloadMap.putAll(item.entrySet().stream()
                    .map(SharedHelper::fromDynamoDbAttributeValue)
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));
        }

        publishResponse(uuid, optionalMessageId, Optional.empty(), responseToken, payloadMap);

        return "done";
    }

    @Override
    public boolean isMessageIdRequired() {
        // Yes, we need the message ID for a get message request
        return true;
    }

    @Override
    public boolean isRecipientUuidRequired() {
        return false;
    }
}
