package io.vertx.fargate.mqtt.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.fargate.interfaces.AdapterVerticle;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class MqttServerAdapterVerticle extends AbstractVerticle implements AdapterVerticle {
    private static final Logger log = LoggerFactory.getLogger(MqttServerAdapterVerticle.class);

    @Inject
    MqttServerOptions mqttServerOptions;
    @Inject
    Handler<MqttEndpoint> mqttEndpointHandler;

    @Inject
    public MqttServerAdapterVerticle() {
    }

    @Override
    public void start() {
        MqttServer mqttServer = MqttServer.create(vertx, mqttServerOptions);

        mqttServer.endpointHandler(mqttEndpointHandler)
                .listen(asyncResult -> logSuccess(mqttServer, asyncResult));

        mqttServer.exceptionHandler(event -> log.error(event.getMessage()));
    }

    private Void logSuccess(MqttServer mqttServer, AsyncResult<MqttServer> asyncResult) {
        if (!asyncResult.succeeded()) {
            log.error("Error on starting the server" + asyncResult.cause().getMessage());
            throw new RuntimeException("Error on starting the server" + asyncResult.cause().getMessage());
        }

        log.info("MQTT server is listening on port " + mqttServer.actualPort());

        return null;
    }
}
