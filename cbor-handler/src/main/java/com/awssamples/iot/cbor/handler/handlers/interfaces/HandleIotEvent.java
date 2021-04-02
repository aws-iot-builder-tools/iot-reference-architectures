package com.awssamples.iot.cbor.handler.handlers.interfaces;

import io.vavr.Lazy;
import io.vavr.control.Try;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Base interface that shares code between the classes that handle IoT messages
 */
public interface HandleIotEvent {
    String OUTPUT_TOPIC_KEY = "OutputTopic";
    DescribeEndpointRequest DESCRIBE_ENDPOINT_REQUEST = DescribeEndpointRequest.builder().endpointType("iot:Data-ATS").build();
    Lazy<String> ENDPOINT_ADDRESS = Lazy.of(() -> IotClient.create().describeEndpoint(DESCRIBE_ENDPOINT_REQUEST).endpointAddress());
    Lazy<URI> ENDPOINT_URI = Lazy.of(() -> Try.of(() -> new URI(String.join("://", "https", ENDPOINT_ADDRESS.get()))).get());
    Lazy<IotDataPlaneClient> IOT_DATA_PLANE_CLIENT = Lazy.of(() -> IotDataPlaneClient.builder().endpointOverride(ENDPOINT_URI.get()).build());

    // Methods that throw exceptions so that the code fails fast when issues come up (values not specified in the environment, etc)

    /**
     * @return the output topic
     */
    default String getOutputTopic() {
        return getEnvironmentVariableOrThrow(OUTPUT_TOPIC_KEY, this::missingOutputTopicException);
    }

    default RuntimeException missingOutputTopicException() {
        throw new RuntimeException("Missing the output topic in the environment, can not continue");
    }

    /**
     * @param payload the binary data to publish
     */
    default void publishResponse(byte[] payload) {
        // Convert the payload map to JSON and then to an SdkBytes object
        SdkBytes sdkBytes = SdkBytes.fromByteArray(payload);

        // Build the publish request
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(getOutputTopic())
                .payload(sdkBytes)
                .build();

        // Publish with the IoT data plane client
        IOT_DATA_PLANE_CLIENT.get().publish(publishRequest);
    }

    /**
     * @param name                     the name of a variable to retrieve from the environment
     * @param runtimeExceptionSupplier the supplier that will throw an exception if the variable is not found
     * @return the value of the variable in the environment
     */
    default String getEnvironmentVariableOrThrow(String name, Supplier<RuntimeException> runtimeExceptionSupplier) {
        return Optional.ofNullable(System.getenv(name)).orElseThrow(runtimeExceptionSupplier);
    }
}
