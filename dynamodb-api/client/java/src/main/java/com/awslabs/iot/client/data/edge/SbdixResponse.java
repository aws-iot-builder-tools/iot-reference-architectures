package com.awslabs.iot.client.data.edge;

import org.immutables.value.Value;

@Value.Immutable
public abstract class SbdixResponse {
    public abstract MoStatus getMoStatus();

    public abstract int getMoMsn();

    public abstract MtStatus getMtStatus();

    public abstract int getMtMsn();

    public abstract int getMtLength();

    public abstract int getMtQueued();
}
