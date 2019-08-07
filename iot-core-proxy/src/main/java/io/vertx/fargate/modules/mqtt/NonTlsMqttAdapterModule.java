package io.vertx.fargate.modules.mqtt;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Handler;
import io.vertx.fargate.interfaces.AdapterVerticle;
import io.vertx.fargate.mqtt.handlers.BasicMqttCallbackHandler;
import io.vertx.fargate.mqtt.handlers.BasicMqttEndpointHandler;
import io.vertx.fargate.mqtt.interfaces.MqttCallbackHandler;
import io.vertx.fargate.mqtt.verticles.MqttServerAdapterVerticle;
import io.vertx.fargate.providers.NonTlsMqttServerOptionsProvider;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServerOptions;

import java.util.Set;

@Module
public abstract class NonTlsMqttAdapterModule {
    @Provides
    public static Set<AdapterVerticle> adapterVerticleSet(MqttServerAdapterVerticle mqttServerAdapterVerticle) {
        return Set.of(mqttServerAdapterVerticle);
    }

    @Provides
    public static MqttServerOptions mqttServerOptions(NonTlsMqttServerOptionsProvider nonTlsMqttServerOptionsProvider) {
        return nonTlsMqttServerOptionsProvider.get();
    }

    @Binds
    abstract Handler<MqttEndpoint> mqttEndpointHandler(BasicMqttEndpointHandler basicMqttEndpointHandler);

    @Binds
    abstract MqttCallbackHandler mqttCallbackHandler(BasicMqttCallbackHandler basicMqttCallbackHandler);
}
