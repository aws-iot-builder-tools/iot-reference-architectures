package io.vertx.fargate.providers.interfaces;

import io.vertx.fargate.data.ScopeDownConfiguration;

import java.util.Map;

public interface ScopeDownPolicyProvider {
    Map generateScopeDownPolicy(ScopeDownConfiguration scopeDownConfiguration);
}
