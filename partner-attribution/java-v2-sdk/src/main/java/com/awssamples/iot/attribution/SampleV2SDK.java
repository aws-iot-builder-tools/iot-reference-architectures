package com.awssamples.iot.attribution;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.iot.model.DescribeEndpointResponse;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

public class SampleV2SDK {
    private static final String PLATFORM = "x-amzn-platform";

    public static void main(String[] args) throws URISyntaxException {
        IotClient iotClient = IotClient.create();

        DescribeEndpointResponse describeEndpointResponse = iotClient.describeEndpoint(DescribeEndpointRequest.builder().endpointType("iot:Data-ATS").build());

        IotDataPlaneClient iotDataPlaneClient = IotDataPlaneClient.builder()
                .endpointOverride(new URI(String.join("", "https://", describeEndpointResponse.endpointAddress())))
                .build();

        AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = AwsRequestOverrideConfiguration.builder()
                .putHeader(PLATFORM, "APN/1 Java2PartnerSoft,ManagedIoT,v1.2.1")
                .build();

        PublishRequest publishRequest = PublishRequest.builder()
                .topic("topic_from_java_v2_sdk")
                .payload(SdkBytes.fromString("payload_from_java_v2_sdk", Charset.defaultCharset()))
                .overrideConfiguration(awsRequestOverrideConfiguration)
                .build();

        iotDataPlaneClient.publish(publishRequest);
    }
}
