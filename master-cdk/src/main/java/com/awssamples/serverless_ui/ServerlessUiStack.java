package com.awssamples.serverless_ui;

import com.aws.samples.lambda.servlet.automation.GeneratedClassInfo;
import com.awssamples.iam.policies.CloudWatchEventsPolicies;
import com.awssamples.iam.policies.LambdaPolicies;
import com.awssamples.stacktypes.JavaGradleStack;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;

import java.util.*;
import java.util.stream.Collectors;

import static com.awssamples.gradle.GradleSupport.HANDLE_REQUEST;
import static com.awssamples.shared.IotHelper.*;
import static com.awssamples.shared.StsHelper.getAssumeRolePolicyStatement;
import static java.util.Collections.singletonList;

public class ServerlessUiStack extends software.amazon.awscdk.core.Stack implements JavaGradleStack {
    private static final Logger log = LoggerFactory.getLogger(ServerlessUiStack.class);
    public static final String ROOT_CATCHALL = "/*";

    private String projectDirectory;
    private String outputArtifactName;
    public final Duration lambdaFunctionTimeout = Duration.seconds(10);
    private final Role dashboardRole;
    private final Role iotRole;
    private final Map<String, String> iotEnvironment;

    public ServerlessUiStack(final Construct parent, final String name) {
        super(parent, name);

        projectDirectory = "../serverless-ui/" + name + "/";
        outputArtifactName = name + "-all.jar";

        // Build all of the necessary JARs
        build();

        dashboardRole = getDashboardLambdaFunctionRole("dashboard");
        iotRole = getIotLambdaFunctionRole("iot");
        Role clientRole = getClientRole("client", iotRole);
        iotEnvironment = new HashMap<>();
        iotEnvironment.put("ClientRole", clientRole.getRoleArn());

        List<Tuple2<GeneratedClassInfo, Function>> classInfoAndFunctionList = getGeneratedClassInfo().stream()
                .map(generatedClassInfo -> new Tuple2<>(generatedClassInfo, getFunction(generatedClassInfo)))
                .collect(Collectors.toList());

        Optional<LambdaRestApi> optionalLambdaRestApi = buildLambdaRestApiIfPossible(classInfoAndFunctionList);

        if (!optionalLambdaRestApi.isPresent()) {
            throw new RuntimeException("Lambda REST API was not built because the root handler could not be found");
        }

        LambdaRestApi lambdaRestApi = optionalLambdaRestApi.get();

        addNonRootFunctions(lambdaRestApi, classInfoAndFunctionList);
    }

    private Optional<LambdaRestApi> buildLambdaRestApiIfPossible(List<Tuple2<GeneratedClassInfo, Function>> classInfoAndFunctionList) {
        // Special case, this handles all of the root stuff
        return classInfoAndFunctionList.stream()
                // Special case, this handles all of the root stuff
                .filter(value -> value._1.path.equals(ROOT_CATCHALL))
                .findFirst()
                .map(value -> value._2)
                .map(this::buildLambdaRestApiForRootFunction);
    }

    private LambdaRestApi buildLambdaRestApiForRootFunction(Function function) {
        return LambdaRestApi.Builder.create(this, "serverless-ui")
                .restApiName("serverless-ui")
                .description("CDK built serverless API")
                .endpointConfiguration(EndpointConfiguration.builder().types(singletonList(EndpointType.EDGE)).build())
                .binaryMediaTypes(singletonList("*/*"))
                .handler(function)
                .build();
    }

    private List<IResource> addNonRootFunctions(LambdaRestApi lambdaRestApi, List<Tuple2<GeneratedClassInfo, Function>> classInfoAndFunctionList) {
        return classInfoAndFunctionList.stream()
                .filter(value -> !value._1.path.equals(ROOT_CATCHALL))
                .map(this::trimLeadingSlashIfNecessary)
                .map(value -> new Tuple3<>(value._1.path, value._2, lambdaRestApi))
                .map(this::buildResource)
                .collect(Collectors.toList());
    }

    private IResource buildResource(Tuple3<String, Function, LambdaRestApi> input) {
        String path = input._1;
        Function function = input._2;
        LambdaRestApi lambdaRestApi = input._3;
        List<String> cumulativePath = new ArrayList<>();

        @NotNull IResource parent = lambdaRestApi.getRoot();

        for (String currentPath : path.split("/")) {
            if (currentPath.equals("*")) {
                // TODO: Implement wildcards other than the root
                continue;
            }

            cumulativePath.add(currentPath);
            String cumulativePathString = String.join("-", cumulativePath);

            IResource resource = parent.getResource(currentPath);

            if (resource == null) {
                // Only create this if it doesn't exist already
                resource = Resource.Builder.create(this, cumulativePathString)
                        .parent(parent)
                        .pathPart(currentPath)
                        .build();
            }

            parent = resource;
        }

        parent.addMethod("ANY", getLambdaIntegration(function));

        return parent;
    }

    private Tuple2<GeneratedClassInfo, Function> trimLeadingSlashIfNecessary(Tuple2<GeneratedClassInfo, Function> value) {
        if (value._1.path.startsWith("/")) {
            // Trim leading slash so it doesn't mess up our split and give us an empty value first
            value._1.path = value._1.path.substring(1);
        }

        return value;
    }

    private LambdaIntegration getLambdaIntegration(Function function) {
        return LambdaIntegration.Builder
                .create(function)
                .build();
    }

    private Function getFunction(GeneratedClassInfo generatedClassInfo) {
        String[] splitClassName = generatedClassInfo.className.split("\\.");
        String lastClassName = splitClassName[splitClassName.length - 1];

        Role role;
        Map<String, String> environment = new HashMap<>();

        if (lastClassName.contains("Static")) {
            role = dashboardRole;
        } else if (lastClassName.contains("Iot")) {
            role = iotRole;
            environment = iotEnvironment;
        } else {
            throw new RuntimeException("Unexpected class name [" + lastClassName + "], couldn't determine role");
        }

        String handlerName = String.join("::", generatedClassInfo.className, HANDLE_REQUEST);

        FunctionProps functionProps = FunctionProps.builder()
                .code(getAssetCode())
                .handler(handlerName)
                .memorySize(1024)
                .timeout(lambdaFunctionTimeout)
                .runtime(Runtime.JAVA_11)
                .environment(environment)
                .role(role)
                .functionName(String.join("-", this.getStackName(), lastClassName))
                .tracing(Tracing.ACTIVE)
                .build();

        return new Function(this, generatedClassInfo.className + "Function", functionProps);
    }

    private Role getDashboardLambdaFunctionRole(String name) {
        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(singletonList(CloudWatchEventsPolicies.getMinimalCloudWatchEventsLoggingPolicy()))
                .build();

        return getRoleAssumedByLambda(name, policyDocumentProps);
    }

    private Role getIotLambdaFunctionRole(String name) {
        PolicyStatement iotPublishPolicyStatement = getPublishToTopicPrefixPolicyStatement(this, "power-podium");
        PolicyStatement iotDescribeEndpointPolicyStatement = getDescribeEndpointPolicyStatement();
        PolicyStatement stsAssumeRolePolicyStatement = getAssumeRolePolicyStatement();

        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(Arrays.asList(CloudWatchEventsPolicies.getMinimalCloudWatchEventsLoggingPolicy(),
                        iotPublishPolicyStatement,
                        iotDescribeEndpointPolicyStatement,
                        stsAssumeRolePolicyStatement))
                .build();

        return getRoleAssumedByLambda(name, policyDocumentProps);
    }

    @NotNull
    private Role getRoleAssumedByLambda(String name, PolicyDocumentProps policyDocumentProps) {
        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        Map<String, PolicyDocument> policyDocuments = new HashMap<>();
        policyDocuments.put("root", policyDocument);

        RoleProps roleProps = RoleProps.builder()
                .assumedBy(LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL)
                .inlinePolicies(policyDocuments)
                .build();

        return new Role(this, name + "Role", roleProps);
    }

    private Role getClientRole(String name, Role assumedByRole) {
        PolicyStatement iotPublishPolicyStatement = getPublishToTopicPrefixPolicyStatement(this, "clients");
        PolicyStatement iotSubscribePolicyStatement = getSubscribeToTopicPrefixPolicyStatement(this, "clients");
        PolicyStatement iotReceivePolicyStatement = getReceiveFromTopicPrefixPolicyStatement(this, "clients");
        PolicyStatement iotConnectPolicyStatement = getConnectAllPolicyStatement();
        PolicyStatement iotDescribeEndpointPolicyStatement = getDescribeEndpointPolicyStatement();

        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(Arrays.asList(CloudWatchEventsPolicies.getMinimalCloudWatchEventsLoggingPolicy(),
                        iotPublishPolicyStatement,
                        iotSubscribePolicyStatement,
                        iotReceivePolicyStatement,
                        iotConnectPolicyStatement,
                        iotDescribeEndpointPolicyStatement))
                .build();

        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        Map<String, PolicyDocument> policyDocuments = new HashMap<>();
        policyDocuments.put("root", policyDocument);

        RoleProps roleProps = RoleProps.builder()
                .assumedBy(assumedByRole)
                .inlinePolicies(policyDocuments)
                .build();

        return new Role(this, name + "Role", roleProps);
    }


    @Override
    public String getProjectDirectory() {
        return projectDirectory;
    }

    @Override
    public String getOutputArtifactName() {
        return outputArtifactName;
    }

}
