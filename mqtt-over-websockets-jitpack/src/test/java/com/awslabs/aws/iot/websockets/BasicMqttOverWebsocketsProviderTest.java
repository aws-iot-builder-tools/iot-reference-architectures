package com.awslabs.aws.iot.websockets;

import com.awslabs.aws.iot.websockets.data.ImmutableClientId;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;

public class BasicMqttOverWebsocketsProviderTest {
    private MqttOverWebsocketsProvider mqttOverWebsocketsProvider;
    private ImmutableClientId clientId;
    private MqttClient mqttClient;
    private MqttMessage randomMqttMessage;
    private String randomMqttTopic;
    private byte[] randomMqttPayload;
    private boolean flag;

    @Before
    public void setup() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, MqttException {
        mqttOverWebsocketsProvider = new BasicMqttOverWebsocketsProvider();
        String uuid = UUID.randomUUID().toString();
        clientId = ImmutableClientId.builder().clientId(uuid).build();
        mqttClient = mqttOverWebsocketsProvider.getMqttClient(clientId);
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
    public void shouldThrowClientIsNotConnectedException() throws MqttException {
        mqttClient.publish(randomMqttTopic, randomMqttMessage);
    }

    /**
     * Attempting to publish after connecting should succeed
     *
     * @throws MqttException
     */
    @Test
    public void shouldPublishMessage() throws MqttException {
        mqttClient.connect();
        mqttClient.publish(randomMqttTopic, randomMqttMessage);
    }

    /**
     * Attempting to publish to a subscribed topic should hit the callback
     *
     * @throws MqttException
     */
    @Test
    public void shouldReceiveMessage() throws MqttException {
        mqttClient.connect();

        mqttClient.subscribe(randomMqttTopic);

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Assert.assertThat(topic, is(randomMqttTopic));
                Assert.assertThat(message.getPayload(), is(randomMqttPayload));
                setFlag(true);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        mqttClient.publish(randomMqttTopic, randomMqttMessage);

        await().atMost(5, SECONDS).until(this::getFlag);

        Assert.assertThat(flag, is(true));
    }

    /**
     * Attempting to publish to a topic other than the subscribed topic should not hit the callback
     *
     * @throws MqttException
     */
    @Test(expected = ConditionTimeoutException.class)
    public void shouldNotReceiveMessage() throws MqttException {
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
