package com.awslabs.aws.iot.resultsiterator;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.CreateThingRequest;
import com.amazonaws.services.iot.model.DeleteThingRequest;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BasicResultsIteratorTest {
    private static final int NUMBER_OF_THINGS_TO_CREATE = 20;
    private List<String> testThingNames = new ArrayList<>();
    private AWSIotClient awsIotClient;

    @Before
    public void setup() {
        for (int loop = 0; loop < NUMBER_OF_THINGS_TO_CREATE; loop++) {
            testThingNames.add(UUID.randomUUID().toString());
        }

        Collections.sort(testThingNames);

        awsIotClient = (AWSIotClient) AWSIotClient.builder().build();

        for (String testThingName : testThingNames) {
            awsIotClient.createThing(new CreateThingRequest().withThingName(testThingName));
        }
    }

    @After
    public void tearDown() {
        for (String testThingName : testThingNames) {
            awsIotClient.deleteThing(new DeleteThingRequest().withThingName(testThingName));
        }
    }

    @Test
    public void shouldGetAllDevicesWithDefaultList() {
        Stream<ThingAttribute> thingAttributeStream = new V1ResultsIterator<ThingAttribute>(awsIotClient, ListThingsRequest.class).stream();

        List<String> thingNames = thingAttributeStream
                .map(ThingAttribute::getThingName)
                .filter(thingName -> testThingNames.contains(thingName))
                .sorted()
                .collect(Collectors.toList());

        assertThat(thingNames, is(testThingNames));
    }
}
