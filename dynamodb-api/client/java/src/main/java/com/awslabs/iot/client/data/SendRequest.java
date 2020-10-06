package com.awslabs.iot.client.data;

import org.apache.commons.codec.binary.Hex;
import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Gson.TypeAdapters
@Value.Immutable
public abstract class SendRequest {
    public abstract long getClientMessageId();

    public abstract String getMessage();

    public abstract String getImei();

    public String toJson() {
        Map<String, Object> output = new HashMap<>();
        output.put("client_message_id", getClientMessageId());
        // Reference: https://en.everybodywiki.com/GSE_Open_GPS_Protocol
        // Octet 0 - 0x00 - Packet version
        // Octet 1 - 0x01 - Text message
        // Octet 2 - XXXX - Length of remaining bytes
        // Octet 3...     - Message
        String hexPayload = "0001" + String.format("%02x", getMessage().length()) + Hex.encodeHexString(getMessage().getBytes());
        // LoggerFactory.getLogger(SendRequest.class).info("hex payload: " + hexPayload);
        output.put("hex_payload", hexPayload);
        output.put("imei", getImei());

        return new com.google.gson.Gson().toJson(output);
    }
}
