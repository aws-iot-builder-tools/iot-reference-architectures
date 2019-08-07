package io.vertx.fargate.modules;

import com.awslabs.resultsiterator.ResultsIteratorModule;
import com.awssamples.iot.mqtt.auth.handlers.AnyClientAuthHandler;
import dagger.Component;
import io.vertx.fargate.applications.NonTlsMqtt;
import io.vertx.fargate.modules.mqtt.NonTlsMqttAdapterModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ResultsIteratorModule.class, BaselineDaggerAwsModule.class, BaselineDaggerIotBrokerModule.class, NonTlsMqttAdapterModule.class})
public interface Injector {
    NonTlsMqtt nonTlsMqtt();

    void inject(AnyClientAuthHandler anyClientAuthHandler);
}
