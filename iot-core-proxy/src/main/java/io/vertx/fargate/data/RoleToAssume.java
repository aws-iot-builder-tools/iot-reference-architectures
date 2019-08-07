package io.vertx.fargate.data;

import org.immutables.value.Value;

@Value.Immutable
public abstract class RoleToAssume {
    public abstract String getName();
}
