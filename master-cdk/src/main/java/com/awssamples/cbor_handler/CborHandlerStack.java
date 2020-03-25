package com.awssamples.cbor_handler;

import com.awslabs.general.helpers.interfaces.LambdaPackagingHelper;
import com.awslabs.lambda.data.ImmutableJavaLambdaFunctionDirectory;
import com.awssamples.MasterApp;
import com.awssamples.shared.IotHelper;
import com.awssamples.shared.LambdaHelper;
import com.awssamples.shared.RoleHelper;
import com.awssamples.shared.RulesEngineSqlHelper;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iot.CfnTopicRule;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;

public class CborHandlerStack extends software.amazon.awscdk.core.Stack {
    public static final String CBOR_MESSAGE = "CborMessage";
    public static final String JSON_MESSAGE = "JsonMessage";
    private static final String HANDLER_PACKAGE = "com.awssamples.iot.cbor.handler.handlers";
    private static final String OUTPUT_TOPIC = "OutputTopic";
    private static final String CBOR_INPUT_TOPIC = String.join("/", "cbor", "input");
    private static final String CBOR_OUTPUT_TOPIC = String.join("/", "json", "output");
    private static final String JSON_INPUT_TOPIC = String.join("/", "json", "input");
    private static final String JSON_OUTPUT_TOPIC = String.join("/", "cbor", "output");
    // Amazon Ion event handler
    private static final String CBOR_EVENT_HANDLER = String.join(".", HANDLER_PACKAGE, "HandleCborEvent");
    // JSON event handler
    private static final String JSON_EVENT_HANDLER = String.join(".", HANDLER_PACKAGE, "HandleJsonEvent");
    private static final Duration LAMBDA_FUNCTION_TIMEOUT = Duration.seconds(10);
    private static final String PROJECT_DIRECTORY = "../cbor-handler/";
    private static final List<File> PROJECT_DIRECTORY_FILES = singletonList(new File(PROJECT_DIRECTORY));
    private static final String BUILD_OUTPUT_DIRECTORY = "build/libs/";
    private static final String OUTPUT_JAR = "cbor-handler-1.0-SNAPSHOT-all.jar";
    private static final String ASSET_NAME = String.join("", PROJECT_DIRECTORY, BUILD_OUTPUT_DIRECTORY, OUTPUT_JAR);

    @Inject
    LambdaPackagingHelper lambdaPackagingHelper;

    public CborHandlerStack(final Construct parent, final String name) {
        super(parent, name);

        // Inject dependencies
        MasterApp.masterInjector.inject(this);

        // Build all of the necessary JARs
        PROJECT_DIRECTORY_FILES.stream()
                .map(directory -> ImmutableJavaLambdaFunctionDirectory.builder().directory(directory).build())
                .forEach(lambdaPackagingHelper::packageJavaFunction);

        // Resources to convert an Amazon Ion message to JSON
        Role cborMessageRole = RoleHelper.buildPublishToTopicIotEventRole(this, CBOR_MESSAGE, CBOR_OUTPUT_TOPIC, EMPTY_LIST);
        Map<String, String> cborLambdaEnvironment = getCborLambdaEnvironment();
        Function cborMessageFunction = LambdaHelper.buildIotEventLambda(this, CBOR_MESSAGE, cborMessageRole, Runtime.JAVA_8, emptyMap(), cborLambdaEnvironment, ASSET_NAME, CBOR_EVENT_HANDLER, LAMBDA_FUNCTION_TIMEOUT);
        CfnTopicRule cborMessageTopicRule = RulesEngineSqlHelper.buildSelectAllBinaryIotEventRule(this, CBOR_MESSAGE, cborMessageFunction, CBOR_INPUT_TOPIC);
        IotHelper.allowIotTopicRuleToInvokeLambdaFunction(this, cborMessageTopicRule, cborMessageFunction, CBOR_MESSAGE);

        // Resources to convert a JSON message to Amazon Ion
        Role jsonMessageRole = RoleHelper.buildPublishToTopicIotEventRole(this, JSON_MESSAGE, JSON_OUTPUT_TOPIC, EMPTY_LIST);
        Map<String, String> jsonLambdaEnvironment = getJsonLambdaEnvironment();
        Function jsonMessageFunction = LambdaHelper.buildIotEventLambda(this, JSON_MESSAGE, jsonMessageRole, Runtime.JAVA_8, emptyMap(), jsonLambdaEnvironment, ASSET_NAME, JSON_EVENT_HANDLER, LAMBDA_FUNCTION_TIMEOUT);
        CfnTopicRule jsonMessageTopicRule = RulesEngineSqlHelper.buildSelectAllIotEventRule(this, JSON_MESSAGE, jsonMessageFunction, JSON_INPUT_TOPIC);
        IotHelper.allowIotTopicRuleToInvokeLambdaFunction(this, jsonMessageTopicRule, jsonMessageFunction, JSON_MESSAGE);
    }

    private Map<String, String> getCborLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(OUTPUT_TOPIC, CBOR_OUTPUT_TOPIC);

        return environment;
    }

    private Map<String, String> getJsonLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(OUTPUT_TOPIC, JSON_OUTPUT_TOPIC);

        return environment;
    }
}
