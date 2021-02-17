package com.awslabs.aws.iot.resultsiterator;

import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ThingAttribute;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BasicResultsIteratorTest {
    private static final int NUMBER_OF_THINGS_TO_CREATE = 20;
    private List<String> testThingNames = List.empty();
    private IotClient iotClient;

    @Before
    public void setup() {
        for (int loop = 0; loop < NUMBER_OF_THINGS_TO_CREATE; loop++) {
            testThingNames = testThingNames.append(UUID.randomUUID().toString());
        }

        testThingNames = testThingNames.sorted();

        iotClient = IotClient.create();

        testThingNames
                .map(testThingName -> CreateThingRequest.builder().thingName(testThingName).build())
                .forEach(createThingRequest -> iotClient.createThing(createThingRequest));
    }

    @After
    public void tearDown() {
        testThingNames
                .map(testThingName -> DeleteThingRequest.builder().thingName(testThingName).build())
                .forEach(deleteThingRequest -> iotClient.deleteThing(deleteThingRequest));
    }

    @Test
    public void shouldGetAllDevicesWithDefaultList() {
        Stream<ThingAttribute> thingAttributeStream = new V2ResultsIterator<ThingAttribute>(iotClient, ListThingsRequest.class).stream();

        List<String> thingNames = thingAttributeStream
                .map(ThingAttribute::thingName)
                .filter(thingName -> testThingNames.contains(thingName))
                .sorted()
                .toList();

        assertThat(thingNames, is(testThingNames));
    }
}
