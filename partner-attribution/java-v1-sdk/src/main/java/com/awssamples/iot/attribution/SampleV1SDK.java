package com.awssamples.iot.attribution;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClientBuilder;
import com.amazonaws.services.iot.model.DescribeEndpointRequest;
import com.amazonaws.services.iot.model.DescribeEndpointResult;
import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.iotdata.model.PublishRequest;

import java.nio.ByteBuffer;

public class SampleV1SDK {
    private static final String PLATFORM = "x-amzn-platform";

    public static void main(String[] args) {
        AWSIot awsIot = AWSIotClientBuilder.defaultClient();

        DescribeEndpointRequest describeEndpointRequest = new DescribeEndpointRequest();
        describeEndpointRequest.setEndpointType("iot:Data-ATS");

        DescribeEndpointResult describeEndpointResult = awsIot.describeEndpoint(describeEndpointRequest);

        String region = new DefaultAwsRegionProviderChain().getRegion();

        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(describeEndpointResult.getEndpointAddress(), region);

        AWSIotData awsIotData = AWSIotDataClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .build();

        PublishRequest publishRequest = new PublishRequest()
                .withTopic("topic_from_java_v1_sdk")
                .withPayload(ByteBuffer.wrap("payload_from_java_v1_sdk".getBytes()));

        publishRequest.putCustomRequestHeader(PLATFORM, "APN/1 Java1PartnerSoft,ManagedIoT,v1.2.1");

        awsIotData.publish(publishRequest);
    }
}
