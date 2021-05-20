package com.awslabs.aws.iot.resultsiterator;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListThingsResponse;
import software.amazon.awssdk.services.iot.model.ThingAttribute;

import java.util.stream.IntStream;

public class Example {
    public static void main(String[] args) {
        // Get the default region
        Region defaultRegion = new DefaultAwsRegionProviderChain().getRegion();
        // Use us-east-1 as our non-default region by default
        Region nonDefaultRegion = Region.US_EAST_1;

        if (defaultRegion.equals(Region.US_EAST_1)) {
            // Use us-east-2 as our non-default region if the default is us-east-1
            nonDefaultRegion = Region.US_EAST_2;
        }

        // Add some whitespace so the output is easier to read
        whitespace();

        // Create an IoT client in the default region. No need to specify the region, just call create.
        IotClient iotClientDefaultRegion = IotClient.create();

        // Create an IoT client in the non-default region. Region must be specified explicitly in the builder.
        IotClient iotClientNonDefaultRegion = IotClient.builder()
                .region(nonDefaultRegion)
                .build();
        System.out.println("Calling APIs in the default region [" + defaultRegion + "]");
        callApis(iotClientDefaultRegion);

        // Add some whitespace so the output is easier to read
        whitespace();

        System.out.println("Calling APIs in the non-default region [" + nonDefaultRegion + "]");
        callApis(iotClientNonDefaultRegion);

        whitespace();
    }

    private static void whitespace() {
        IntStream.range(0, 5).forEach(value -> System.out.println());
    }

    private static void callApis(IotClient iotClient) {
        System.out.println("\tRetrieving the first page of results from the IoT listThings API");
        ListThingsResponse listThingsResponse = iotClient.listThings();

        if (!listThingsResponse.hasThings()) {
            System.out.println("\tNo things found in the default region");
        } else {
            listThingsResponse.things().stream()
                    .map(ThingAttribute::thingName)
                    .forEach(thingName -> System.out.println("\t\t" + thingName));
        }
    }
}
