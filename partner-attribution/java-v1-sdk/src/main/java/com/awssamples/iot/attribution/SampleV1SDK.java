package com.awssamples.iot.attribution;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.iotdata.model.PublishRequest;

import java.nio.ByteBuffer;

public class SampleV1SDK {
    private static final String SDK = "SDK";
    private static final String PLATFORM = "Platform";

    public static void main(String[] args) {
        AWSIotData awsIotData = AWSIotDataClientBuilder.defaultClient();

        PublishRequest publishRequest = new PublishRequest()
                .withTopic("topic_from_java_v1_sdk")
                .withPayload(ByteBuffer.wrap("payload_from_java_v1_sdk".getBytes()));

        publishRequest.putCustomRequestHeader(SDK, "my_sdk_java_v1_sdk");
        publishRequest.putCustomRequestHeader(PLATFORM, "my_platform_java_v1_sdk,00:11:22:33:44:55,serial_number");

        awsIotData.publish(publishRequest);
    }
}
