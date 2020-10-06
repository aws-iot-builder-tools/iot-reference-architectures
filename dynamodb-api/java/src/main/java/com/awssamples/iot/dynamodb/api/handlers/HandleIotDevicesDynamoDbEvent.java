package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.List;
import java.util.stream.Collectors;

public class HandleIotDevicesDynamoDbEvent implements HandleIotDevicesEvent {
    @Override
    public List<String> getDevices() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(SharedHelper.getTableName())
                .attributesToGet(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        return scanResponse.items().stream()
                .map(item -> item.get(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME))
                .map(AttributeValue::s)
                .distinct()
                .collect(Collectors.toList());
    }
}
