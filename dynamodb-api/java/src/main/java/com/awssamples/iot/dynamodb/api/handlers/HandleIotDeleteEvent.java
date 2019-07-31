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
    public String innerHandle(String responseToken, Optional<String> optionalUuid, Optional<String> optionalMessageId) {
        String uuid = optionalUuid.get();
        String messageId = optionalMessageId.get();

        // Delete the row with the exact UUID and message ID values
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(SharedHelper.UUID, AttributeValue.builder().s(uuid).build());
        key.put(SharedHelper.MESSAGE_ID, AttributeValue.builder().s(messageId).build());

        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                .tableName(SharedHelper.getTableName())
                .key(key)
                .build();
        DynamoDbClient.create().deleteItem(deleteItemRequest);

        // Return a payload on the response topic that contains the UUID and deleted message ID. This response only
        //   indicates that a record was deleted if it was present. If the record was not present then this the previous
        //   delete operation is a NOOP.
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put(SharedHelper.UUID, uuid);
        payloadMap.put(SharedHelper.MESSAGE_ID, messageId);

        publishResponse(responseToken, payloadMap);

        return "done";
    }

    @Override
    public boolean isMessageIdRequired() {
        // Yes, we need the message ID for a delete message request
        return true;
    }

    @Override
    public boolean isUuidRequired() {
        // Yes, we need the UUID for a delete message request
        return true;
    }
}
