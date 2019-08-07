package io.vertx.fargate.mqtt.interfaces;

import io.vertx.mqtt.MqttEndpoint;
import org.eclipse.paho.client.mqttv3.MqttCallback;

public interface MqttCallbackHandler extends MqttCallback {
    void setMqttEndpoint(MqttEndpoint mqttEndpoint);
}
