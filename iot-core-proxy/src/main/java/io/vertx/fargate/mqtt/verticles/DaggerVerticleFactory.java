package io.vertx.fargate.mqtt.verticles;

import io.vavr.collection.Stream;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.fargate.interfaces.AdapterVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vertx.fargate.modules.BaselineDaggerIotBrokerModule.VERTICLE_COUNTER;

public class DaggerVerticleFactory implements VerticleFactory {
    // If you ask for a class with this prefix we'll instantiate it via Guice
    public static final String PREFIX = "java-dagger";
    public static final Logger log = LoggerFactory.getLogger(DaggerVerticleFactory.class);

    // Get a provider that can give us a new set of adapter Verticles for each request
    @Inject
    Provider<Set<AdapterVerticle>> adapterVerticleSetProvider;
    @Inject
    @Named(VERTICLE_COUNTER)
    AtomicInteger verticleCounter = new AtomicInteger(0);

    @Inject
    public DaggerVerticleFactory() {
    }

    @Override
    public String prefix() {
        return PREFIX;
    }

    @Override
    public void createVerticle(String name, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
        // Some guidance from: https://github.com/dinstone/agate/blob/702378317df192efe2fba85ce9c40d611267b39f/agate-gateway/src/main/java/com/dinstone/agate/gateway/context/AgateVerticleFactory.java
        promise.complete(() -> createVerticle(name));
    }

    private Verticle createVerticle(String name) {
        // Remove the prefix so we can get to the simple class name
        String shortName = VerticleFactory.removePrefix(name);

        // Find the class that ends with the short name we've derived.  In our case MqttClient will resolve BasicMqttClient.
        return Stream.ofAll(adapterVerticleSetProvider.get())
                .filter(verticle -> verticle.getClass().getName().endsWith(shortName))
                .headOption()
                .peek(v -> log.info("Instantiated [" + shortName + "], instance #" + verticleCounter.incrementAndGet()))
                .getOrElseThrow(() -> new RuntimeException("Verticle WAS NOT found for [" + shortName + "]"));
    }
}
