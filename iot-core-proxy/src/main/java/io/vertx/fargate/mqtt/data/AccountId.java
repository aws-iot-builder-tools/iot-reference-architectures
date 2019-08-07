package io.vertx.fargate.mqtt.data;

import org.immutables.value.Value;

@Value.Immutable
public abstract class AccountId {
    public abstract String getAccountId();
}
