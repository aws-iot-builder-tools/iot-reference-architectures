package com.awssamples.shared;

import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;

import java.util.HashMap;
import java.util.Map;

public class LambdaHelper {
    public static Function buildIotEventLambda(Stack stack, String functionNamePrefix, Role role, Runtime runtime, Map<String, String> defaultEnvironment, Map<String, String> additionalEnvironment, String assetName, String handler, Duration lambdaFunctionTimeout) {
        return buildIotEventLambda(stack, functionNamePrefix, role, runtime, defaultEnvironment, additionalEnvironment, Code.fromAsset(assetName), handler, lambdaFunctionTimeout);
    }

    public static Function buildIotEventLambda(Stack stack, String functionNamePrefix, Role role, Runtime runtime, Map<String, String> defaultEnvironment, Map<String, String> additionalEnvironment, AssetCode assetCode, String handler, Duration lambdaFunctionTimeout) {
        Map<String, String> environment = new HashMap<>(defaultEnvironment);
        environment.putAll(additionalEnvironment);

        FunctionProps functionProps = FunctionProps.builder()
                .code(assetCode)
                .handler(handler)
                .memorySize(1024)
                .timeout(lambdaFunctionTimeout)
                .environment(environment)
                .runtime(runtime)
                .role(role)
                .build();

        return new Function(stack, functionNamePrefix + "Lambda", functionProps);
    }
}
