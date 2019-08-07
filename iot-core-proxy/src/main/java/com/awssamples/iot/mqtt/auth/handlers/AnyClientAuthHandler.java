package com.awssamples.iot.mqtt.auth.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.vertx.fargate.data.ImmutableScopeDownConfiguration;
import io.vertx.fargate.data.ScopeDownConfiguration;
import io.vertx.fargate.modules.DaggerInjector;
import io.vertx.fargate.modules.Injector;
import io.vertx.fargate.providers.interfaces.ScopeDownConfigurationProvider;
import io.vertx.fargate.providers.interfaces.ScopeDownPolicyProvider;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnyClientAuthHandler implements RequestHandler<Map, Map> {
    public static final List<String> ALLOW_ALL = Collections.singletonList("*");
    private static final Injector injector = DaggerInjector.create();
    @Inject
    ScopeDownConfigurationProvider scopeDownConfigurationProvider;

    @Inject
    ScopeDownPolicyProvider scopeDownPolicyProvider;

    @Override
    public Map handleRequest(final Map input, final Context context) {
        injector.inject(this);

        ScopeDownConfiguration scopeDownConfiguration = ImmutableScopeDownConfiguration.builder()
                .configuration(scopeDownConfigurationProvider.generateScopeDownConfiguration(
                        ALLOW_ALL,
                        ALLOW_ALL,
                        ALLOW_ALL,
                        ALLOW_ALL))
                .build();

        return scopeDownPolicyProvider.generateScopeDownPolicy(scopeDownConfiguration);
    }
}
