package io.vertx.fargate.mqtt.data;

import io.vertx.mqtt.MqttEndpoint;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.immutables.value.Value;

@Value.Immutable
public abstract class MqttClientAndEndpoint {
    public abstract MqttClient getMqttClient();

    public abstract MqttEndpoint getMqttEndpoint();
}
