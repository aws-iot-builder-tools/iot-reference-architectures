package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;

public class HandleIotGetEvent implements HandleIotEvent {
    private static final String MESSAGE_NOT_AVAILABLE = "Message not available";

    @Override
    public String getOperationType() {
        return "get";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, Option<String> uuidOption, Option<String> messageIdOption, Option<String> recipientIdOption) {
        String messageId = messageIdOption.get();
        String uuid = uuidOption.get();

        // Get the row with the exact UUID and message ID values
        HashMap<String, AttributeValue> key = HashMap.of(
                SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(uuid).build(),
                SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(messageId).build());

        DynamoDbClient dynamoDbClient = DynamoDbClient.create();

        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(SharedHelper.getTableName())
                .key(key.toJavaMap())
                .build();
        GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);

        HashMap<String, AttributeValue> item = HashMap.ofAll(getItemResponse.item());

        // Return a payload on the response topic that contains the UUID and message ID
        HashMap<String, Object> payloadMap = HashMap.of(
                SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuidOption,
                SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, messageId);

        if (item.isEmpty()) {
            // The message was not found, include an error message
            payloadMap = payloadMap.put(SharedHelper.ERROR_KEY, MESSAGE_NOT_AVAILABLE);
        } else {
            // The message was found, merge it into our payload
            payloadMap = payloadMap.merge(item);
        }

        publishResponse(uuidOption, messageIdOption, Option.none(), responseToken, payloadMap);

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

    @Override
    public boolean isDeviceUuidRequired() {
        return true;
    }
}
