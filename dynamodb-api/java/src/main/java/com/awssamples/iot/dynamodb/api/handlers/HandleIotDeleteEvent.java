package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HandleIotDeleteEvent implements HandleIotEvent {
    @Override
    public String getOperationType() {
        return "delete";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, String uuid, Optional<String> optionalMessageId, Optional<String> optionalRecipientId) {
        String messageId = optionalMessageId.get();

        // Delete the row with the exact UUID and message ID values
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(uuid).build());
        key.put(SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(messageId).build());

        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                .tableName(SharedHelper.getTableName())
                .key(key)
                .build();
        DynamoDbClient.create().deleteItem(deleteItemRequest);

        // Return a payload on the response topic that contains the UUID and deleted message ID. This response only
        //   indicates that a record was deleted if it was present. If the record was not present then this the previous
        //   delete operation is a NOOP.
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuid);
        payloadMap.put(SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, messageId);

        publishResponse(uuid, optionalMessageId, Optional.empty(), responseToken, payloadMap);

        return "done";
    }

    @Override
    public boolean isMessageIdRequired() {
        // Yes, we need the message ID for a delete message request
        return true;
    }

    @Override
    public boolean isRecipientUuidRequired() {
        return false;
    }
}
