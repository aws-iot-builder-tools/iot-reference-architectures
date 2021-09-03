package com.awssamples.iot.dynamodb.api.handlers;

import io.vavr.collection.List;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.SearchIndexRequest;
import software.amazon.awssdk.services.iot.model.SearchIndexResponse;
import software.amazon.awssdk.services.iot.model.ThingDocument;

import static com.awssamples.iot.dynamodb.api.SharedHelper.THING_GROUP;

public class HandleIotDevicesRegistryEvent implements HandleIotDevicesEvent {
    public static final String DELIMITER = ":";
    public static final String THING_GROUP_NAMES = "thingGroupNames";

    @Override
    public List<String> getDevices() {
        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString(String.join(DELIMITER, THING_GROUP_NAMES, THING_GROUP))
                .build();

        SearchIndexResponse searchIndexResponse = IotClient.create().searchIndex(searchIndexRequest);

        /*
        Map<String, Optional<Map>> devices = searchIndexResponse.things().stream()
                .map(thingDocument -> new Tuple2<>(thingDocument.thingName(), Optional.ofNullable(getGson().fromJson(thingDocument.shadow(), Map.class))))
                .collect(Collectors.toMap(tuple2 -> tuple2._1, tuple2 -> tuple2._2));
         */

        return List.ofAll(searchIndexResponse.things())
                .map(ThingDocument::thingName);
    }

    @Override
    public boolean isDeviceUuidRequired() {
        return false;
    }
}
