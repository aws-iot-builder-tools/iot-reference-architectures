package io.vertx.fargate.mqtt.data;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

public class RejectedConnectionException extends Exception {
    private final MqttConnectReturnCode mqttConnectReturnCode;

    public RejectedConnectionException(String errorMessage, MqttConnectReturnCode mqttConnectReturnCode) {
        super(errorMessage);
        this.mqttConnectReturnCode = mqttConnectReturnCode;
    }

    public MqttConnectReturnCode getMqttConnectReturnCode() {
        return mqttConnectReturnCode;
    }
}
