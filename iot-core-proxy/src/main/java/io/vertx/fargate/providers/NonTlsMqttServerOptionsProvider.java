package io.vertx.fargate.providers;

import io.vertx.mqtt.MqttServerOptions;

import javax.inject.Inject;
import javax.inject.Provider;

public class NonTlsMqttServerOptionsProvider implements Provider<MqttServerOptions> {
    @Inject
    public NonTlsMqttServerOptionsProvider() {
    }

    @Override
    public MqttServerOptions get() {
        return new MqttServerOptions()
                .setHost("0.0.0.0")
                .setPort(1883);
    }
}
