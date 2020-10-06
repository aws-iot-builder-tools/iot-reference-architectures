package com.awslabs.iot.client.parameters.interfaces;

import java.util.List;

public interface ParameterExtractor {
    List<String> getParameters(String input);
}
