package com.awssamples.iot.cbor.handler.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.awssamples.iot.cbor.handler.handlers.interfaces.HandleIotEvent;
import com.upokecenter.cbor.CBORObject;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;

public class HandleCborEvent implements HandleIotEvent, RequestHandler<Map, byte[]> {
    /**
     * Receiving a CBOR message that we need to convert to JSON
     *
     * @param input the raw binary input wrapped in a Map
     * @return
     */
    @Override
    public byte[] handleRequest(Map input, Context context) {
        byte[] binaryData = Base64.getDecoder().decode((String) input.get("data"));
        CBORObject cborObject = CBORObject.Read(new ByteArrayInputStream(binaryData));
        byte[] payload = cborObject.ToJSONString().getBytes();
        publishResponse(payload);
        return payload;
    }
}
