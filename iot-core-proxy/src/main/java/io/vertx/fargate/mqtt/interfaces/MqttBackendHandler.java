package io.vertx.fargate.mqtt.interfaces;

import com.awslabs.aws.iot.websockets.MqttOverWebsocketsProvider;
import com.awslabs.aws.iot.websockets.data.MqttClientConfig;
import io.vavr.control.Try;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public interface MqttBackendHandler {
    default MqttClient createBackendConnection(MqttOverWebsocketsProvider mqttOverWebsocketsProvider, MqttCallback mqttCallback, MqttClientConfig mqttClientConfig) throws MqttException {
        // Get the websockets URI for the broker (uses the scope down configuration, if available)
        MqttClient mqttClient = Try.of(() -> mqttOverWebsocketsProvider.getMqttClient(mqttClientConfig)).get();

        mqttClient.setCallback(mqttCallback);

        mqttOverWebsocketsProvider.connect(mqttClient);

        return mqttClient;
    }
}
