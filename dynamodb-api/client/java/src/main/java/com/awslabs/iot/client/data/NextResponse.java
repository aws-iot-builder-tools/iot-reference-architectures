package com.awslabs.iot.client.data;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Optional;

@Gson.TypeAdapters
@Value.Immutable
public abstract class NextResponse {
    public abstract String getSpecifiedMessageId();

    public abstract Optional<String> getNextMessageId();

    public abstract String getUuid();

    public abstract Optional<String> getError();
}
