package com.awslabs.iot.client.helpers.json.interfaces;

public interface ObjectPrettyPrinter {
    String prettyPrint(String json);

    String prettyPrint(Object object);
}
