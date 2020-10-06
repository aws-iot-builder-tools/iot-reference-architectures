package com.awslabs.iot.client.data;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Optional;

@Gson.TypeAdapters
@Value.Immutable
public abstract class QueryResponse {
    public abstract Optional<String> getOldestMessageId();

    public abstract Optional<String> getNewestMessageId();

    public abstract Optional<String> getError();

    public abstract String getUuid();
}
