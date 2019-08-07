package com.awssamples.iot.mqtt.auth.handlers;

import dagger.Binds;
import dagger.Module;
import io.vertx.fargate.providers.BasicScopeDownConfigurationProvider;
import io.vertx.fargate.providers.BasicScopeDownPolicyProvider;
import io.vertx.fargate.providers.interfaces.ScopeDownConfigurationProvider;
import io.vertx.fargate.providers.interfaces.ScopeDownPolicyProvider;

@Module
public abstract class AnyClientAuthModule {
    @Binds
    abstract ScopeDownConfigurationProvider scopeDownConfigurationProvider(BasicScopeDownConfigurationProvider basicScopeDownConfigurationProvider);

    @Binds
    abstract ScopeDownPolicyProvider scopeDownPolicyProvider(BasicScopeDownPolicyProvider basicScopeDownPolicyProvider);
}
