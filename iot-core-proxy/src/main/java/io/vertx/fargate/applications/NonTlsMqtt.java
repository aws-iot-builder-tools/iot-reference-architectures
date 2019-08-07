package io.vertx.fargate.applications;

import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.fargate.IotBrokerFargate;
import io.vertx.fargate.interfaces.AdapterVerticle;
import io.vertx.fargate.modules.DaggerInjector;
import io.vertx.fargate.modules.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

public class NonTlsMqtt implements IotBrokerFargate {
    private static final Logger log = LoggerFactory.getLogger(NonTlsMqtt.class);
    @Inject
    VerticleFactory verticleFactory;
    @Inject
    Provider<Set<AdapterVerticle>> adapterVerticlesProvider;
    @Inject
    Vertx vertx;

    @Inject
    public NonTlsMqtt() {
    }

    public static void main(String[] args) {
        Injector injector = DaggerInjector.create();

        injector.nonTlsMqtt().run();
    }

    @Override
    public VerticleFactory getVerticleFactory() {
        return verticleFactory;
    }

    @Override
    public Provider<Set<AdapterVerticle>> getAdapterVerticlesProvider() {
        return adapterVerticlesProvider;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public void initialize() {
        // Do nothing
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }
}
