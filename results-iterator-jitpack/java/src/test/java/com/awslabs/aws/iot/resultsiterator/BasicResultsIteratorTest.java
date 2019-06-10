package com.awslabs.aws.iot.resultsiterator;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<ThingAttribute> thingAttributeList = new ResultsIterator<ThingAttribute>(awsIotClient, ListThingsRequest.class, ListThingsResult.class).iterateOverResults();

        List<String> thingNames = thingAttributeList.stream()
                .map(ThingAttribute::getThingName)
                .filter(thingName -> testThingNames.contains(thingName))
                .sorted()
                .collect(Collectors.toList());

        Assert.assertThat(thingNames, is(testThingNames));
    }
}
