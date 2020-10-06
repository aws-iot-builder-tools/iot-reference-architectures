package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import io.vavr.Tuple2;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.SearchIndexRequest;
import software.amazon.awssdk.services.iot.model.SearchIndexResponse;
import software.amazon.awssdk.services.iot.model.ThingDocument;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.awssamples.iot.dynamodb.api.SharedHelper.IRIDIUM;
import static com.awssamples.iot.dynamodb.api.SharedHelper.getGson;

public class HandleIotDevicesRegistryEvent implements HandleIotDevicesEvent {

    public static final String DELIMITER = ":";
    public static final String THING_GROUP_NAMES = "thingGroupNames";

    @Override
    public List<String> getDevices() {
        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString(String.join(DELIMITER, THING_GROUP_NAMES, IRIDIUM))
                .build();

        SearchIndexResponse searchIndexResponse = IotClient.create().searchIndex(searchIndexRequest);

        /*
        Map<String, Optional<Map>> devices = searchIndexResponse.things().stream()
                .map(thingDocument -> new Tuple2<>(thingDocument.thingName(), Optional.ofNullable(getGson().fromJson(thingDocument.shadow(), Map.class))))
                .collect(Collectors.toMap(tuple2 -> tuple2._1, tuple2 -> tuple2._2));
         */

        return searchIndexResponse.things().stream()
                .map(ThingDocument::thingName)
                .collect(Collectors.toList());
    }
}
