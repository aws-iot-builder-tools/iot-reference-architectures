package com.awssamples.iot_publish_test;

import com.aws.samples.cdk.constructs.iam.policies.CloudWatchLogsPolicies;
import com.aws.samples.cdk.constructs.iam.policies.LambdaPolicies;
import com.awssamples.shared.IotPolicies;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IotPublishTestStack extends software.amazon.awscdk.core.Stack {
    private static final Logger log = LoggerFactory.getLogger(IotPublishTestStack.class);
    private static final Path currentWorkingDirectory = Paths.get("").toAbsolutePath();
    private static final Path parentDirectory = currentWorkingDirectory.getParent();
    private static final Path projectDirectory = parentDirectory.resolve("iot-publish-test");

    public IotPublishTestStack(final Construct parent, final String name) {
        super(parent, name);

        Role baseRole = getBaseRole("base", LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL);
        Function function = buildFunction(baseRole, baseRole.getRoleArn(), "base");
        buildOutput(function, "base");

        Role assumeAnyRole = getBaseRole("assumeAny", LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL);
        assumeAnyRole.attachInlinePolicy(getAssumeAnyRolePolicy("assumeAny"));
        Function assumeAnyFunction = buildFunction(assumeAnyRole, assumeAnyRole.getRoleArn(), "assumeAny");
        buildOutput(assumeAnyFunction, "assumeAny");

        Role assumeAny2Role = getBaseRole("assumeAny2", LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL);
        assumeAny2Role.attachInlinePolicy(getAssumeAnyRolePolicy("assumeAny2"));
        AccountPrincipal accountPrincipal = new AccountPrincipal("541589084637");
        Role assumeAny3Role = getBaseRole("assumeAny3", accountPrincipal);
        Function assumeAny2Function = buildFunction(assumeAny2Role, assumeAny3Role.getRoleArn(), "assumeAny3");
        buildOutput(assumeAny2Function, "assumeAny2");
    }

    private Policy getAssumeAnyRolePolicy(String name) {
        PolicyStatementProps policyStatementProps = PolicyStatementProps.builder()
                .actions(Collections.singletonList("sts:AssumeRole"))
                .effect(Effect.ALLOW)
                .resources(Collections.singletonList("*"))
                .build();
        PolicyStatement policyStatement = new PolicyStatement(policyStatementProps);
        PolicyProps policyProps = PolicyProps.builder()
                .statements(Collections.singletonList(policyStatement))
                .build();
        return new Policy(this, name + "Policy", policyProps);
    }

    private Role getBaseRole(String name, IPrincipal assumedBy) {
        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(List.of(CloudWatchLogsPolicies.minimalCloudWatchEventsLoggingPolicy).asJava())
                .build();
        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        Map<String, PolicyDocument> policyDocuments = new HashMap<>();
        policyDocuments.put("root", policyDocument);

        RoleProps baseRoleProps = RoleProps.builder()
                .assumedBy(assumedBy)
                .inlinePolicies(policyDocuments)
                // For publish, subscribe, receive
                .managedPolicies(Collections.singletonList(IotPolicies.AWS_IOT_DATA_ACCESS_POLICY))
                .build();

        return new Role(this, name + "Role", baseRoleProps);
    }

    private Function buildFunction(Role role, String roleArnToAssume, String name) {
        Map<String, String> environment = new HashMap<>();
        environment.put("RoleToAssume", roleArnToAssume);

        FunctionProps functionProps = FunctionProps.builder()
                .code(Code.fromAsset(projectDirectory.resolve("iot_publish.zip").toString()))
                .handler(String.join(".", "iot_publish", "lambda_handler"))
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .runtime(Runtime.PYTHON_2_7)
                .role(role)
                .environment(environment)
                .build();

        return new Function(this, name + "Function", functionProps);
    }

    private void buildOutput(Function function, String name) {
        CfnOutputProps cfnOutputProps = CfnOutputProps.builder()
                .value(function.getFunctionArn())
                .build();

        CfnOutput cfnOutput = new CfnOutput(this, name + "Output", cfnOutputProps);
    }
}
