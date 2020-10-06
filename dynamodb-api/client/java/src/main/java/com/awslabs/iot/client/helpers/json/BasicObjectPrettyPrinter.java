package com.awslabs.iot.client.helpers.json;

import com.awslabs.iot.client.helpers.json.interfaces.ObjectPrettyPrinter;
import com.google.gson.*;

import javax.inject.Inject;

public class BasicObjectPrettyPrinter implements ObjectPrettyPrinter {
    @Inject
    public BasicObjectPrettyPrinter() {
    }

    @Override
    public String prettyPrint(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        return gson.toJson(je);
    }

    @Override
    public String prettyPrint(Object object) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                // Don't print SDK related metadata
                return (fieldAttributes.getName().startsWith("sdk") && fieldAttributes.getName().endsWith("Metadata"));
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).create();

        return prettyPrint(gson.toJson(object));
    }
}
