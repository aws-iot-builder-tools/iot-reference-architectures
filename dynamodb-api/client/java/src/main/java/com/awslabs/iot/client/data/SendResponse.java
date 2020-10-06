package com.awslabs.iot.client.data;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Map;

@Gson.TypeAdapters
@Value.Immutable
public abstract class SendResponse {
    public abstract String getSqsMessageId();

    public abstract String getImei();

    public static SendResponse fromMap(Map map) {
        return ImmutableSendResponse.builder()
                .sqsMessageId((String) map.get("sqs_message_id"))
                .imei((String) map.get("uuid"))
                .build();
    }
}
