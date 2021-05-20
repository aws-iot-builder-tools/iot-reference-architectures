package com.awslabs.aws.iot.websockets;

import com.awslabs.aws.iot.websockets.data.ImmutableClientId;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BasicMqttOverWebsocketsProviderTest {
    private MqttOverWebsocketsProvider mqttOverWebsocketsProvider;
    private ImmutableClientId clientId;
    private MqttClient defaultRegionMqttClient;
    private MqttClient nonDefaultRegionMqttClient;
    private MqttMessage randomMqttMessage;
    private String randomMqttTopic;
    private byte[] randomMqttPayload;
    private boolean flag;

    @Before
    public void setup() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, MqttException {
        mqttOverWebsocketsProvider = new BasicMqttOverWebsocketsProvider();
        String uuid = UUID.randomUUID().toString();
        clientId = ImmutableClientId.builder().clientId(uuid).build();
        defaultRegionMqttClient = mqttOverWebsocketsProvider.getMqttClient(clientId);

        // Get the default region
        Region defaultRegion = new DefaultAwsRegionProviderChain().getRegion();

        // Use us-east-1 as our non-default region by default
        Region nonDefaultRegion = Region.US_EAST_1;

        if (defaultRegion.equals(Region.US_EAST_1)) {
            // Use us-east-2 as our non-default region if the default is us-east-1
            nonDefaultRegion = Region.US_EAST_2;
        }

        nonDefaultRegionMqttClient = mqttOverWebsocketsProvider.getMqttClient(clientId, nonDefaultRegion);

        setFlag(false);

        randomMqttTopic = UUID.randomUUID().toString();
        randomMqttPayload = UUID.randomUUID().toString().getBytes();
        randomMqttMessage = new MqttMessage(randomMqttPayload);
    }

    /**
     * Attempting to publish without connecting should fail
     *
     * @throws MqttException
     */
    @Test(expected = MqttException.class)
    public void shouldThrowClientIsNotConnectedException1() throws MqttException {
        defaultRegionMqttClient.publish(randomMqttTopic, randomMqttMessage);
    }

    @Test(expected = MqttException.class)
    public void shouldThrowClientIsNotConnectedException2() throws MqttException {
        nonDefaultRegionMqttClient.publish(randomMqttTopic, randomMqttMessage);
    }

    /**
     * Attempting to publish after connecting should succeed
     *
     * @throws MqttException
     */
    @Test
    public void shouldPublishMessage1() throws MqttException {
        defaultRegionMqttClient.connect();
        defaultRegionMqttClient.publish(randomMqttTopic, randomMqttMessage);
    }

    @Test
    public void shouldPublishMessage2() throws MqttException {
        nonDefaultRegionMqttClient.connect();
        nonDefaultRegionMqttClient.publish(randomMqttTopic, randomMqttMessage);
    }

    /**
     * Attempting to publish to a subscribed topic should hit the callback
     *
     * @throws MqttException
     */
    @Test
    public void shouldReceiveMessage1() throws MqttException {
        innerShouldReceiveMessage(defaultRegionMqttClient);
    }

    @Test
    public void shouldReceiveMessage2() throws MqttException {
        innerShouldReceiveMessage(nonDefaultRegionMqttClient);
    }

    private void innerShouldReceiveMessage(MqttClient mqttClient) throws MqttException {
        mqttClient.connect();

        mqttClient.subscribe(randomMqttTopic);

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                assertThat(topic, is(randomMqttTopic));
                assertThat(message.getPayload(), is(randomMqttPayload));
                setFlag(true);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        mqttClient.publish(randomMqttTopic, randomMqttMessage);

        await().atMost(5, SECONDS).until(this::getFlag);

        assertThat(flag, is(true));
    }

    /**
     * Attempting to publish to a topic other than the subscribed topic should not hit the callback
     *
     * @throws MqttException
     */
    @Test(expected = ConditionTimeoutException.class)
    public void shouldNotReceiveMessage1() throws MqttException {
        innerShouldNotReceiveMessage(defaultRegionMqttClient);
    }

    @Test(expected = ConditionTimeoutException.class)
    public void shouldNotReceiveMessage2() throws MqttException {
        innerShouldNotReceiveMessage(nonDefaultRegionMqttClient);
    }

    private void innerShouldNotReceiveMessage(MqttClient mqttClient) throws MqttException {
        mqttClient.connect();

        String otherRandomMqttTopic = UUID.randomUUID().toString();

        mqttClient.subscribe(otherRandomMqttTopic);

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                setFlag(false);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        mqttClient.publish(randomMqttTopic, randomMqttMessage);

        await().atMost(5, SECONDS).until(this::getFlag);
    }

    private boolean getFlag() {
        return flag;
    }

    private void setFlag(boolean value) {
        flag = value;
    }
}
