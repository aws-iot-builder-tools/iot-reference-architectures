package com.awssamples.client;

public class SharedWithServer {
    public static final String topicPrefix = String.join("/", "clients", "jwt");

    public static final String topicMqttWildcard = String.join("/", topicPrefix, "#");
}
