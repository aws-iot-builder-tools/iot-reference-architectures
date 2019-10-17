package com.awssamples.amazon_ion_handler;

import com.awssamples.shared.*;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iot.CfnTopicRule;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyMap;

public class AmazonIonHandlerStack extends software.amazon.awscdk.core.Stack {
    private static final String ION_MESSAGE = "IonMessage";
    private static final String JSON_MESSAGE = "JsonMessage";
    private static final String OUTPUT_TOPIC = "OutputTopic";
    private static final String ION_INPUT_TOPIC = String.join("/", "ion", "input");
    private static final String ION_OUTPUT_TOPIC = String.join("/", "json", "output");
    private static final String JSON_INPUT_TOPIC = String.join("/", "json", "input");
    private static final String JSON_OUTPUT_TOPIC = String.join("/", "ion", "output");
    private static final String ION_HANDLER_SCRIPT_NAME = "Ion";
    private static final String JSON_HANDLER_SCRIPT_NAME = "Json";
    private static final String DUAL_DEPLOYMENT_PACKAGE_NAME = "package";
    // Amazon Ion event handler
    private static final String ION_EVENT_HANDLER = String.join(".", ION_HANDLER_SCRIPT_NAME, "function_handler");
    // JSON event handler
    private static final String JSON_EVENT_HANDLER = String.join(".", JSON_HANDLER_SCRIPT_NAME, "function_handler");
    private static final Duration LAMBDA_FUNCTION_TIMEOUT = Duration.seconds(10);
    private static final String PROJECT_DIRECTORY = "../amazon-ion-handler/";
    private static final File PROJECT_DIRECTORY_FILE = new File(PROJECT_DIRECTORY);

    public AmazonIonHandlerStack(final Construct parent, final String name) {
        super(parent, name);

        // Build all of the necessary JARs
        Path dualDeploymentPackage = PythonSupport.buildZip(PROJECT_DIRECTORY_FILE, DUAL_DEPLOYMENT_PACKAGE_NAME);

        // Resources to convert an Amazon Ion message to JSON
        Role ionMessageRole = RoleHelper.buildPublishToTopicIotEventRole(this, ION_MESSAGE, ION_OUTPUT_TOPIC, EMPTY_LIST);
        Map<String, String> ionLambdaEnvironment = getIonLambdaEnvironment();
        Function ionMessageFunction = LambdaHelper.buildIotEventLambda(this, ION_MESSAGE, ionMessageRole, Runtime.PYTHON_3_7, emptyMap(), ionLambdaEnvironment, dualDeploymentPackage.toString(), ION_EVENT_HANDLER, LAMBDA_FUNCTION_TIMEOUT);
        CfnTopicRule ionMessageTopicRule = RulesEngineSqlHelper.buildSelectAllBinaryIotEventRule(this, ION_MESSAGE, ionMessageFunction, ION_INPUT_TOPIC);
        IotHelper.allowIotTopicRuleToInvokeLambdaFunction(this, ionMessageTopicRule, ionMessageFunction, ION_MESSAGE);

        // Resources to convert a JSON message to Amazon Ion
        Role jsonMessageRole = RoleHelper.buildPublishToTopicIotEventRole(this, JSON_MESSAGE, JSON_OUTPUT_TOPIC, EMPTY_LIST);
        Map<String, String> jsonLambdaEnvironment = getJsonLambdaEnvironment();
        Function jsonMessageFunction = LambdaHelper.buildIotEventLambda(this, JSON_MESSAGE, jsonMessageRole, Runtime.PYTHON_3_7, emptyMap(), jsonLambdaEnvironment, dualDeploymentPackage.toString(), JSON_EVENT_HANDLER, LAMBDA_FUNCTION_TIMEOUT);
        CfnTopicRule jsonMessageTopicRule = RulesEngineSqlHelper.buildSelectAllIotEventRule(this, JSON_MESSAGE, jsonMessageFunction, JSON_INPUT_TOPIC);
        IotHelper.allowIotTopicRuleToInvokeLambdaFunction(this, jsonMessageTopicRule, jsonMessageFunction, JSON_MESSAGE);
    }

    private Map<String, String> getIonLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(OUTPUT_TOPIC, ION_OUTPUT_TOPIC);

        return environment;
    }

    private Map<String, String> getJsonLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(OUTPUT_TOPIC, JSON_OUTPUT_TOPIC);

        return environment;
    }
}
