package com.awssamples.iot.dynamodb.api.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupsForThingRequest;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;

import java.time.Instant;
import java.util.Map;

import static com.awssamples.iot.dynamodb.api.SharedHelper.*;

public class HandleIotNotificationEvent implements RequestHandler<Map, String> {
    private static final Logger log = LoggerFactory.getLogger(HandleIotNotificationEvent.class);

    /**
     * Shared request handler that invokes the different implementations of this class
     *
     * @param input   the input map from Lambda
     * @param context the context from Lambda
     * @return "done" when complete
     * @throws RuntimeException if any part of the request experiences a failure
     */
    @Override
    public String handleRequest(final Map input, final Context context) {
        if ((input == null) || (!input.containsKey(UUID_DYNAMO_DB_COLUMN_NAME))) {
            throw new RuntimeException("Message cannot be handled. It does not contain a UUID.");
        }

        String uuid = (String) input.get(UUID_DYNAMO_DB_COLUMN_NAME);

        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(uuid)
                .build();

        // Try to create the thing but we don't care if it fails, we will just log it
        Try.of(() -> IotClient.create().createThing(createThingRequest))
                .onFailure(throwable -> log.warn("Create thing failed for [" + uuid + "] [" + throwable.getMessage() + "]"));

        CreateThingGroupRequest createThingGroupRequest = CreateThingGroupRequest.builder()
                .thingGroupName(THING_GROUP)
                .build();

        // Try to create the thing group but we don't care if it fails, we will just log it
        Try.of(() -> IotClient.create().createThingGroup(createThingGroupRequest))
                .onFailure(throwable -> log.warn("Create thing group failed for [" + THING_GROUP + "] [" + throwable.getMessage() + "]"));

        UpdateThingGroupsForThingRequest updateThingGroupsForThingRequest = UpdateThingGroupsForThingRequest.builder()
                .thingGroupsToAdd(THING_GROUP)
                .thingName(uuid)
                .build();

        // Try to create the thing group but we don't care if it fails, we will just log it
        Try.of(() -> IotClient.create().updateThingGroupsForThing(updateThingGroupsForThingRequest))
                .onFailure(throwable -> log.warn("Adding thing [" + uuid + "] to group [" + THING_GROUP + "] failed [" + throwable.getMessage() + "]"));

        String shadowJson = "{\"state\":{\"reported\":{\"" + LAST_CONTACT + "\":" + Instant.now().toEpochMilli() + "}}}";

        UpdateThingShadowRequest updateThingShadowRequest = UpdateThingShadowRequest.builder()
                .thingName(uuid)
                .payload(SdkBytes.fromUtf8String(shadowJson))
                .build();

        // Try to update the thing shadow but we don't care if it fails, we will just log it
        Try.of(() -> IotDataPlaneClient.create().updateThingShadow(updateThingShadowRequest))
                .onFailure(throwable -> log.warn("Update shadow failed for [" + uuid + "] [" + throwable.getMessage() + "]"));

        return "done";
    }
}
