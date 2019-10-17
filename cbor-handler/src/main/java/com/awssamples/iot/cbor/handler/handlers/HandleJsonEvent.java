package com.awssamples.iot.cbor.handler.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.awssamples.iot.cbor.handler.handlers.interfaces.HandleIotEvent;
import com.upokecenter.cbor.CBORObject;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HandleJsonEvent implements HandleIotEvent, RequestStreamHandler {
    /**
     * Receiving a JSON message that we need to convert to CBOR
     *
     * @param input the raw binary input
     * @return
     */
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        CBORObject cborObject = CBORObject.FromJSONString(new String(IoUtils.toByteArray(input)));
        byte[] payload = cborObject.EncodeToBytes();
        publishResponse(payload);
        output.write(payload);
    }
}
