package io.vertx.fargate.mqtt.handlers;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.fargate.mqtt.interfaces.MqttCallbackHandler;
import io.vertx.mqtt.MqttEndpoint;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.inject.Inject;

public class BasicMqttCallbackHandler implements MqttCallbackHandler {
    /**
     * The endpoint to communicate with the client (the device)
     */
    private MqttEndpoint mqttEndpoint;

    @Inject
    public BasicMqttCallbackHandler() {
    }

    @Override
    public void connectionLost(Throwable throwable) {
        mqttEndpoint.close();
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        // When we receive a message republish it to our MQTT endpoint (the client connected to us)
        mqttEndpoint.publish(topic, Buffer.buffer(mqttMessage.getPayload()), MqttQoS.valueOf(mqttMessage.getQos()), false, false);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        // Do nothing
    }

    @Override
    public void setMqttEndpoint(MqttEndpoint mqttEndpoint) {
        this.mqttEndpoint = mqttEndpoint;
    }
}
