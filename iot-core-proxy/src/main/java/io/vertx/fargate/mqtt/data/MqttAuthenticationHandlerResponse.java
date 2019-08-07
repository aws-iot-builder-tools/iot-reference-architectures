package io.vertx.fargate.mqtt.data;

import com.awslabs.aws.iot.websockets.data.ClientId;
import io.vavr.control.Option;
import io.vertx.fargate.data.ScopeDownConfiguration;
import org.immutables.value.Value;

@Value.Immutable
public abstract class MqttAuthenticationHandlerResponse {
    public abstract Option<ClientId> getClientId();

    public abstract Option<ScopeDownConfiguration> getScopeDownConfiguration();
}
