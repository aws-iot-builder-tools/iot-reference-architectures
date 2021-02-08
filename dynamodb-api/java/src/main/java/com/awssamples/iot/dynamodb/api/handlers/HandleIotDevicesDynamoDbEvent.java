package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import io.vavr.collection.List;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class HandleIotDevicesDynamoDbEvent implements HandleIotDevicesEvent {
    @Override
    public List<String> getDevices() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(SharedHelper.getTableName())
                .attributesToGet(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        return List.ofAll(scanResponse.items())
                .map(item -> item.get(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME))
                .map(AttributeValue::s)
                .distinct();
    }

    @Override
    public boolean isDeviceUuidRequired() {
        return false;
    }
}
