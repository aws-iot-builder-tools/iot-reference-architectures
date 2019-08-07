package com.awssamples.iot_core_proxy;

import com.aws.samples.cdk.constructs.iam.permissions.IamPermission;
import com.aws.samples.cdk.constructs.iam.permissions.lambda.actions.ImmutableInvoke;
import com.aws.samples.cdk.constructs.iam.permissions.lambda.resources.ImmutableFunction;
import com.aws.samples.cdk.constructs.iam.policies.CloudWatchLogsPolicies;
import com.aws.samples.cdk.constructs.iam.policies.EcsTaskPolicies;
import com.aws.samples.cdk.constructs.iam.policies.LambdaPolicies;
import com.awssamples.shared.CloudWatchPolicies;
import com.awssamples.shared.FargateHelper;
import com.awssamples.shared.GradleHelper;
import com.awssamples.shared.IotPolicies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class IotCoreProxyStack extends software.amazon.awscdk.core.Stack {
    public static final String AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE = "AWS_ACCOUNT_ID";
    public static final String ROLE_TO_ASSUME_ENVIRONMENT_VARIABLE = "RoleToAssume";
    public static final String AUTHENTICATION_FUNCTION_LIST_ENVIRONMENT_VARIABLE = "AuthenticationFunctionList";
    private static final String HANDLER_PACKAGE = "com.awssamples.iot.mqtt.auth.handlers";
    private static final String ANY_CLIENT_AUTH_HANDLER = String.join(".", HANDLER_PACKAGE, "AnyClientAuthHandler");
    private static final String HANDLE_REQUEST = "handleRequest";
    //    private static final String ECR_REPOSITORY_NAME = "iot-core-proxy";
    private static final Logger log = LoggerFactory.getLogger(IotCoreProxyStack.class);
    private static final Path currentWorkingDirectory = Paths.get("").toAbsolutePath();
    private static final Path parentDirectory = currentWorkingDirectory.getParent();
    private static final Path projectDirectory = parentDirectory.resolve("iot-core-proxy/java-with-fargate/java");
    private static final String buildOutputDirectory = "/build/libs/";
    private static final String outputJar = "vertx.jar";
    private static final Path dockerfile = Paths.get("Dockerfile.nonTls");
    private static final List<File> projectDirectoryFiles = Collections.singletonList(projectDirectory.toFile());
    private static final String authenticationFunctionListString = Optional.ofNullable(System.getenv("AUTHENTICATION_FUNCTION_LIST")).orElse("");
    private static final List<String> authenticationFunctionList = authenticationFunctionListString.isEmpty() ? new ArrayList<>() : Arrays.asList(authenticationFunctionListString.split(","));
    private static final String STACK_NAME_ENVIRONMENT_VARIABLE = "StackName";
    private static final Protocol elbv2Tcp = Protocol.TCP;
    private static final software.amazon.awscdk.services.ecs.Protocol ecsTcp = software.amazon.awscdk.services.ecs.Protocol.TCP;

    public IotCoreProxyStack(final Construct parent, final String name) {
        super(parent, name);

        // Build all of the necessary JARs
        projectDirectoryFiles.forEach(GradleHelper::buildJar);

//        String imageId = Try.of(() -> dockerHelper.buildDockerContainer(projectDirectory, dockerfile)).get();
//        Try.of(() -> dockerHelper.pushDockerContainer(ECR_REPOSITORY_NAME, imageId, imageId)).get();

        Vpc vpc = createVpc();

        Cluster cluster = createEcsCluster(vpc);

        Role fargateTaskRole = getFargateTaskRole();

        Role iotPublishRole = getIotPublishRole(fargateTaskRole);

        FargateTaskDefinition fargateTaskDefinition = getFargateTaskDefinition(fargateTaskRole);

//        ContainerImage containerImage = getContainerImage(ECR_REPOSITORY_NAME, imageId);
        AssetImageProps assetImageProps = AssetImageProps.builder()
                .file(dockerfile.toString())
                .build();
        ContainerImage containerImage = ContainerImage.fromAsset(projectDirectory.toString(), assetImageProps);

        if (authenticationFunctionList.size() == 0) {
            log.warn("No authentication functions specified, allowing any client to connect");
            Role anyClientAuthRole = buildAnyClientAuthRole();
            Function anyClientAuthFunction = buildAnyClientAuthFunction(anyClientAuthRole);
            authenticationFunctionList.add(anyClientAuthFunction.getFunctionName());

            ImmutableFunction function = ImmutableFunction.builder().functionName(anyClientAuthFunction.getFunctionName()).build();
            fargateTaskRole.addToPrincipalPolicy(ImmutableInvoke.builder().function(function).build().getPolicyStatement());
        } else {
            // Use the CDK constructs library to convert the function names into policy statements
            List<PolicyStatement> additionalFargateTaskRolePolicyStatements = authenticationFunctionList.stream()
                    .map(functionName -> ImmutableFunction.builder().functionName(functionName).build())
                    .map(function -> ImmutableInvoke.builder().function(function).build())
                    .map(IamPermission::getPolicyStatement)
                    .collect(Collectors.toList());

            // Add all of the new policy statements
            additionalFargateTaskRolePolicyStatements.forEach(fargateTaskRole::addToPrincipalPolicy);
        }

        ContainerDefinition containerDefinition = getContainerDefinition(fargateTaskDefinition, containerImage, iotPublishRole, authenticationFunctionList);

        containerDefinition.addPortMappings(PortMapping.builder().containerPort(1883).hostPort(1883).protocol(ecsTcp).build());

        NetworkLoadBalancer networkLoadBalancer = getNetworkLoadBalancer(vpc);

        HealthCheck healthCheck = getHealthCheck();

        NetworkListener networkListener = getNetworkListener(networkLoadBalancer);

        FargateService fargateService = getFargateService(vpc, cluster, fargateTaskDefinition);

        getNetworkTargetGroup(healthCheck, networkListener, fargateService);

        buildOutputs(networkLoadBalancer);
    }

    private void buildOutputs(NetworkLoadBalancer networkLoadBalancer) {
        CfnOutputProps cfnOutputProps = CfnOutputProps.builder()
                .value(networkLoadBalancer.getLoadBalancerDnsName())
                .build();

        CfnOutput cfnOutput = new CfnOutput(this, "NLBEndpoint", cfnOutputProps);
    }

    private String functionNameToFunctionArn(String functionName) {
        LambdaClient lambdaClient = LambdaClient.create();

        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(functionName)
                .build();

        GetFunctionResponse getFunctionResponse = lambdaClient.getFunction(getFunctionRequest);

        return getFunctionResponse.configuration().functionName();
    }

    private Function buildAnyClientAuthFunction(Role role) {
        File outputJarFile = new File(String.join("", projectDirectory.toString(), buildOutputDirectory, outputJar));

        if (!outputJarFile.exists()) {
            throw new RuntimeException("Output JAR file [" + outputJarFile.getAbsolutePath() + "] does not exist, can not continue");
        }

        Map<String, String> environment = new HashMap<>();
        environment.put(AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE, getAccount());

        FunctionProps functionProps = FunctionProps.builder()
                .code(Code.fromAsset(String.join("", projectDirectory.toString(), buildOutputDirectory, outputJar)))
                .handler(String.join("::", ANY_CLIENT_AUTH_HANDLER, HANDLE_REQUEST))
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .runtime(Runtime.JAVA_11)
                .role(role)
                .environment(environment)
                .build();

        return new Function(this, "AnyClientAuthLambda", functionProps);
    }

    private Role buildAnyClientAuthRole() {
        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(Collections.singletonList(CloudWatchLogsPolicies.minimalCloudWatchEventsLoggingPolicy))
                .build();
        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        Map<String, PolicyDocument> policyDocuments = new HashMap<>();
        policyDocuments.put("root", policyDocument);

        RoleProps anyClientAuthRoleProps = RoleProps.builder()
                .assumedBy(LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL)
                .inlinePolicies(policyDocuments)
                .build();

        return new Role(this, "AnyClientAuthRole", anyClientAuthRoleProps);
    }

    private Role getIotPublishRole(Role taskRole) {
        List<IManagedPolicy> managedPolicies = new ArrayList<>();
        // For publish, subscribe, receive
        managedPolicies.add(IotPolicies.AWS_IOT_DATA_ACCESS_POLICY);

        RoleProps roleProps = RoleProps.builder()
                .managedPolicies(managedPolicies)
                .assumedBy(taskRole)
                .build();
        return new Role(this, "IotPublishRole", roleProps);
    }

    private Role getFargateTaskRole() {
        PolicyStatementProps policyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(Collections.singletonList("sts:AssumeRole"))
                .resources(Collections.singletonList("*"))
                .build();

        PolicyStatement policyStatement = new PolicyStatement(policyStatementProps);

        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(Collections.singletonList(policyStatement))
                .build();

        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        List<IManagedPolicy> managedPolicies = new ArrayList<>();
        // For describe endpoint
        managedPolicies.add(IotPolicies.AWS_IOT_CONFIG_READ_ONLY_ACCESS_POLICY);
        // For CloudWatch metrics
        managedPolicies.add(CloudWatchPolicies.CLOUD_WATCH_FULL_ACCESS_POLICY);

        Map<String, PolicyDocument> inlinePolicies = new HashMap<>();
        inlinePolicies.put("AssumeRole", policyDocument);

        RoleProps roleProps = RoleProps.builder()
                .managedPolicies(managedPolicies)
                .inlinePolicies(inlinePolicies)
                .assumedBy(EcsTaskPolicies.ECS_TASK_PRINCIPAL)
                .build();
        return new Role(this, "TaskRole", roleProps);
    }

    private NetworkTargetGroup getNetworkTargetGroup(HealthCheck healthCheck, NetworkListener networkListener, FargateService fargateService) {
        AddNetworkTargetsProps addNetworkTargetsProps = AddNetworkTargetsProps.builder()
                .targets(Collections.singletonList(fargateService))
                .healthCheck(healthCheck)
                .port(1883)
                .build();

        return networkListener.addTargets("FargateNetworkTarget", addNetworkTargetsProps);
    }

    private NetworkListener getNetworkListener(NetworkLoadBalancer networkLoadBalancer) {
        return networkLoadBalancer.addListener("NetworkListener", NetworkListenerProps.builder()
                .loadBalancer(networkLoadBalancer)
                .port(1883)
                .protocol(elbv2Tcp)
                .build());
    }

    private NetworkLoadBalancer getNetworkLoadBalancer(Vpc vpc) {
        NetworkLoadBalancerProps networkLoadBalancerProps = NetworkLoadBalancerProps.builder()
                .vpc(vpc)
                .internetFacing(true)
                .build();

        return new NetworkLoadBalancer(this, "NetworkLoadBalancer", networkLoadBalancerProps);
    }

    private HealthCheck getHealthCheck() {
        return HealthCheck.builder()
                .protocol(elbv2Tcp)
                .port("1883")
                .build();
    }

    private FargateService getFargateService(Vpc vpc, Cluster cluster, FargateTaskDefinition fargateTaskDefinition) {
        SecurityGroupProps securityGroupProps = SecurityGroupProps.builder()
                .vpc(vpc)
                .build();
        SecurityGroup securityGroup = new SecurityGroup(this, "SecurityGroup", securityGroupProps);
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(1883));

        FargateServiceProps fargateServiceProps = FargateServiceProps.builder()
                .cluster(cluster)
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(5)
                .minHealthyPercent(50)
                .maxHealthyPercent(150)
                .assignPublicIp(false)
                .securityGroup(securityGroup)
                .build();

        return new FargateService(this, "FargateService", fargateServiceProps);
    }

    private ContainerDefinition getContainerDefinition(FargateTaskDefinition fargateTaskDefinition, ContainerImage containerImage, Role iotRole, List<String> authenticationFunctionList) {
        AwsLogDriverProps awsLogDriverProps = AwsLogDriverProps.builder()
                .streamPrefix("ecs")
                .build();

        LogDriver logDriver = LogDriver.awsLogs(awsLogDriverProps);

        Map<String, String> environment = new HashMap<>();
        environment.put(ROLE_TO_ASSUME_ENVIRONMENT_VARIABLE, iotRole.getRoleName());
        environment.put(AUTHENTICATION_FUNCTION_LIST_ENVIRONMENT_VARIABLE, String.join(",", authenticationFunctionList));
        environment.put(AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE, getAccount());
        environment.put(STACK_NAME_ENVIRONMENT_VARIABLE, getStackName());

        ContainerDefinitionProps containerDefinitionProps = ContainerDefinitionProps.builder()
                .taskDefinition(fargateTaskDefinition)
                .image(containerImage)
                .environment(environment)
                .logging(logDriver)
                .build();

        return fargateTaskDefinition.addContainer("Container", containerDefinitionProps);
    }

    private FargateTaskDefinition getFargateTaskDefinition(Role taskRole) {
        FargateTaskDefinitionProps.Builder fargateTaskDefinitionPropsBuilder = FargateHelper.getValidMemoryAndCpu(FargateHelper.VCPU.Quarter, 2048);
        FargateTaskDefinitionProps fargateTaskDefinitionProps = fargateTaskDefinitionPropsBuilder
                .taskRole(taskRole)
                .build();

        return new FargateTaskDefinition(this, "FargateTaskDefinition", fargateTaskDefinitionProps);
    }

    private ContainerImage getContainerImage(String repositoryName, String shortTag) {
        IRepository repository = Repository.fromRepositoryName(this, "Repository", repositoryName);

        return ContainerImage.fromEcrRepository(repository, shortTag);
    }

    private Cluster createEcsCluster(Vpc vpc) {
        ClusterProps clusterProps = ClusterProps.builder()
                .vpc(vpc)
                .build();

        return new Cluster(this, "Cluster", clusterProps);
    }

    private Vpc createVpc() {
        VpcProps vpcProps = VpcProps.builder().build();

        return new Vpc(this, "Vpc", vpcProps);
    }
}
