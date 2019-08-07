package io.vertx.fargate;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.fargate.interfaces.AdapterVerticle;
import io.vertx.fargate.mqtt.verticles.DaggerVerticleFactory;
import org.slf4j.Logger;

import javax.inject.Provider;
import java.util.Set;

public interface IotBrokerFargate {
    Logger getLogger();

    VerticleFactory getVerticleFactory();

    Provider<Set<AdapterVerticle>> getAdapterVerticlesProvider();

    void initialize();

    default void run() {
        initialize();

        // Register our Guice verticle factory
        getVertx().registerVerticleFactory(getVerticleFactory());

        getAdapterVerticlesProvider().get()
                .forEach(this::deployVerticle);
    }

    default void deployVerticle(AdapterVerticle adapterVerticle) {
        String simpleName = adapterVerticle.getClass().getSimpleName();

        getLogger().info("Deploying [" + simpleName + "]");

        // Build the deployment globalOptions to deploy the right number of instances
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setInstances(16);

        // Deploy the Verticle with the GuiceVerticleFactory prefix so Guice instantiates it with all of its dependencies
        getVertx().deployVerticle(String.join(":", DaggerVerticleFactory.PREFIX, simpleName), deploymentOptions);
    }

    Vertx getVertx();
}
