package awslabs.client;

public class SharedWithServer {
    public static final String topicPrefix = String.join("/", "clients", "vendingmachine");

    public static final String topicMqttWildcard = String.join("/", topicPrefix, "#");
}
