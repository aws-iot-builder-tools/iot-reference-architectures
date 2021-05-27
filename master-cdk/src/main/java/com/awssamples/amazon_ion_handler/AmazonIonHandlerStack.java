package com.awssamples.amazon_ion_handler;

import com.aws.samples.cdk.constructs.iam.policies.LambdaPolicies;
import com.aws.samples.cdk.helpers.IotHelper;
import com.aws.samples.cdk.helpers.LambdaHelper;
import com.aws.samples.cdk.helpers.RoleHelper;
import com.aws.samples.cdk.helpers.RulesEngineSqlHelper;
import com.awslabs.general.helpers.interfaces.LambdaPackagingHelper;
import com.awslabs.lambda.data.ImmutablePythonLambdaFunctionDirectory;
import com.awslabs.lambda.data.PythonLambdaFunctionDirectory;
import com.awssamples.MasterApp;
import com.awssamples.stacktypes.PythonStack;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iot.CfnTopicRule;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;

public class AmazonIonHandlerStack extends Stack implements PythonStack {
    private static final String ION_MESSAGE = "IonMessage";
    private static final String JSON_MESSAGE = "JsonMessage";
    private static final String OUTPUT_TOPIC = "OutputTopic";
    private static final String ION_INPUT_TOPIC = String.join("/", "ion", "input");
    private static final String ION_OUTPUT_TOPIC = String.join("/", "json", "output");
    private static final String JSON_INPUT_TOPIC = String.join("/", "json", "input");
    private static final String JSON_OUTPUT_TOPIC = String.join("/", "ion", "output");
    private static final String ION_HANDLER_SCRIPT_NAME = "Ion";
    private static final String JSON_HANDLER_SCRIPT_NAME = "Json";
    // Amazon Ion event handler
    private static final String ION_EVENT_HANDLER = String.join(".", ION_HANDLER_SCRIPT_NAME, "function_handler");
    // JSON event handler
    private static final String JSON_EVENT_HANDLER = String.join(".", JSON_HANDLER_SCRIPT_NAME, "function_handler");

    @Inject
    LambdaPackagingHelper lambdaPackagingHelper;

    public AmazonIonHandlerStack(final Construct parent, final String name) {
        super(parent, name);

        // Inject dependencies
        MasterApp.masterInjector.inject(this);

        // Build all of the necessary code
        Path dualDeploymentPackage = build(this, lambdaPackagingHelper);

        // Build the properties required for both Lambda functions
        FunctionProps.Builder lambdaFunctionPropsBuilder = FunctionProps.builder()
                .runtime(Runtime.PYTHON_3_8)
                .memorySize(128)
                .timeout(Duration.seconds(10));

        // Resources to convert an Amazon Ion message to JSON
        Role ionMessageRole = RoleHelper.buildPublishToTopicRole(this, ION_MESSAGE, ION_OUTPUT_TOPIC, List.empty(), List.empty(), LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL);
        Map<String, String> ionLambdaEnvironment = getIonLambdaEnvironment();
        Function ionMessageFunction = LambdaHelper.buildLambda(this, ION_MESSAGE, ionMessageRole, ionLambdaEnvironment, dualDeploymentPackage.toString(), ION_EVENT_HANDLER, Option.of(lambdaFunctionPropsBuilder));
        CfnTopicRule ionMessageTopicRule = RulesEngineSqlHelper.buildSelectAllBinaryIotEventRule(this, ION_MESSAGE, ionMessageFunction, ION_INPUT_TOPIC);
        IotHelper.allowIotTopicRuleToInvokeLambdaFunction(this, ionMessageTopicRule, ionMessageFunction, ION_MESSAGE);

        // Resources to convert a JSON message to Amazon Ion
        Role jsonMessageRole = RoleHelper.buildPublishToTopicRole(this, JSON_MESSAGE, JSON_OUTPUT_TOPIC, List.empty(), List.empty(), LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL);
        Map<String, String> jsonLambdaEnvironment = getJsonLambdaEnvironment();
        Function jsonMessageFunction = LambdaHelper.buildLambda(this, JSON_MESSAGE, jsonMessageRole, jsonLambdaEnvironment, dualDeploymentPackage.toString(), JSON_EVENT_HANDLER, Option.of(lambdaFunctionPropsBuilder));
        CfnTopicRule jsonMessageTopicRule = RulesEngineSqlHelper.buildSelectAllIotEventRule(this, JSON_MESSAGE, jsonMessageFunction, JSON_INPUT_TOPIC);
        IotHelper.allowIotTopicRuleToInvokeLambdaFunction(this, jsonMessageTopicRule, jsonMessageFunction, JSON_MESSAGE);
    }

    private Map<String, String> getIonLambdaEnvironment() {
        return HashMap.of(OUTPUT_TOPIC, ION_OUTPUT_TOPIC);
    }

    private Map<String, String> getJsonLambdaEnvironment() {
        return HashMap.of(OUTPUT_TOPIC, JSON_OUTPUT_TOPIC);
    }

    @Override
    public String getProjectDirectory() {
        return "../amazon-ion-handler/";
    }
}
