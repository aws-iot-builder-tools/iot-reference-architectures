package io.vertx.fargate.modules;

import com.awslabs.aws.iot.websockets.BasicMqttOverWebsocketsProvider;
import com.awslabs.aws.iot.websockets.MqttOverWebsocketsProvider;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vavr.collection.List;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.fargate.mqtt.verticles.DaggerVerticleFactory;
import io.vertx.fargate.providers.BasicCredentialsProvider;
import io.vertx.fargate.providers.BasicScopeDownConfigurationProvider;
import io.vertx.fargate.providers.BasicScopeDownPolicyProvider;
import io.vertx.fargate.providers.interfaces.CredentialsProvider;
import io.vertx.fargate.providers.interfaces.ScopeDownConfigurationProvider;
import io.vertx.fargate.providers.interfaces.ScopeDownPolicyProvider;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Module
public abstract class BaselineDaggerIotBrokerModule {
    public static final String AUTH_INVOCATION_COUNTER = "AUTH_INVOCATION_COUNTER";
    public static final String VERTICLE_COUNTER = "VERTICLE_COUNTER";

    @Provides
    @Singleton
    public static Vertx vertx() {
        VertxOptions vertxOptions = new VertxOptions();
        // Force the code to print out a stack trace, not just a warning, if anything blocks for more than 2 seconds
        vertxOptions
                .setWarningExceptionTime(2)
                .setWarningExceptionTimeUnit(TimeUnit.SECONDS);

        return Vertx.vertx(vertxOptions);
    }

    @Provides
    @Singleton
    public static ExecutorService authExecutorService() {
        return Executors.newFixedThreadPool(5);
    }

    @Provides
    @Singleton
    @Named(AUTH_INVOCATION_COUNTER)
    public static AtomicInteger authInvocationCounter() {
        return new AtomicInteger(0);
    }

    @Provides
    @Singleton
    @Named(VERTICLE_COUNTER)
    public static AtomicInteger verticleCounter() {
        return new AtomicInteger(0);
    }

    @Binds
    abstract VerticleFactory verticleFactory(DaggerVerticleFactory daggerVerticleFactory);

    @Provides
    public static List<MqttQoS> mqttQoSList() {
        return getSupportedQosList();
    }

    @Binds
    abstract ScopeDownPolicyProvider scopeDownPolicyProvider(BasicScopeDownPolicyProvider basicScopeDownPolicyProvider);

    @Binds
    abstract ScopeDownConfigurationProvider scopeDownConfigurationProvider(BasicScopeDownConfigurationProvider basicScopeDownConfigurationProvider);

    @Binds
    abstract CredentialsProvider credentialsProvider(BasicCredentialsProvider basicCredentialsProvider);

    @Binds
    abstract MqttOverWebsocketsProvider mqttOverWebsocketsProvider(BasicMqttOverWebsocketsProvider basicMqttOverWebsocketsProvider);

    private static List<MqttQoS> getSupportedQosList() {
        // Only supporting QoS 0, not supporting QoS 1 or QoS 2
        return List.of(MqttQoS.AT_MOST_ONCE);
    }
}
