package io.vertx.fargate.data;

import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@Value.Immutable
public abstract class ScopeDownConfiguration {
    public abstract Map<String, List<String>> getConfiguration();
}
