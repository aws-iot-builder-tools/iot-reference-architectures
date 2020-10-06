package com.awslabs.iot.client.data.edge;

import org.immutables.value.Value;

@Value.Immutable
public abstract class SbdsResponse {
    public abstract MoFlag getMoFlag();

    public abstract int getMoMsn();

    public abstract MtFlag getMtFlag();

    public abstract int getMtMsn();
}
