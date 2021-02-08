package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

import java.util.Map;

public class HandleIotDeleteEvent implements HandleIotEvent {
    @Override
    public String getOperationType() {
        return "delete";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, Option<String> uuidOption, Option<String> messageIdOption, Option<String> recipientIdOption) {
        String messageId = messageIdOption.get();
        String uuid = uuidOption.get();

        // Delete the row with the exact UUID and message ID values
        HashMap<String, AttributeValue> key = HashMap.of(
                SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(uuid).build(),
                SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, AttributeValue.builder().s(messageId).build());

        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                .tableName(SharedHelper.getTableName())
                .key(key.toJavaMap())
                .build();
        DynamoDbClient.create().deleteItem(deleteItemRequest);

        // Return a payload on the response topic that contains the UUID and deleted message ID. This response only
        //   indicates that a record was deleted if it was present. If the record was not present then this the previous
        //   delete operation is a NOOP.
        HashMap<String, String> payloadMap = HashMap.of(
                SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuid,
                SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME, messageId);

        publishResponse(uuidOption, messageIdOption, Option.none(), responseToken, payloadMap);

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

    @Override
    public boolean isDeviceUuidRequired() {
        return true;
    }
}
