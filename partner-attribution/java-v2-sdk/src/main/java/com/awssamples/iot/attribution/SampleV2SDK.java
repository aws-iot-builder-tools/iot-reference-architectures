package com.awssamples.iot.attribution;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import java.nio.charset.Charset;

public class SampleV2SDK {
    private static final String SDK = "SDK";
    private static final String PLATFORM = "Platform";

    public static void main(String[] args) {
        IotDataPlaneClient iotDataPlaneClient = IotDataPlaneClient.create();

        AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = AwsRequestOverrideConfiguration.builder()
                .putHeader(SDK, "my_sdk_java_v2_sdk")
                .putHeader(PLATFORM, "my_platform_java_v2_sdk,00:11:22:33:44:55,serial_number")
                .build();

        PublishRequest publishRequest = PublishRequest.builder()
                .topic("topic_from_java_v2_sdk")
                .payload(SdkBytes.fromString("payload_from_java_v2_sdk", Charset.defaultCharset()))
                .overrideConfiguration(awsRequestOverrideConfiguration)
                .build();

        iotDataPlaneClient.publish(publishRequest);
    }
}
