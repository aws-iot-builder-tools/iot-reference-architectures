package com.awslabs.aws.iot.websockets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import software.amazon.awssdk.regions.Region;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public interface MqttOverWebsocketsProvider {
    MqttClient getMqttClient(String clientId) throws MqttException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException;

    MqttClient getMqttClient(String clientId, Optional<Region> optionalRegion, Optional<String> optionalEndpointAddress) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, MqttException;

    void connect(MqttClient mqttClient) throws MqttException;

    // Derived from: http://docs.aws.amazon.com/iot/latest/developerguide/iot-dg.pdf
    String getMqttOverWebsocketsUri(Optional<Region> optionalRegion, Optional<String> optionalEndpointAddress) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException;
}
