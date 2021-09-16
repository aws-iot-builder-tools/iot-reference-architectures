package com.awssamples.fargate;

import com.aws.samples.cdk.constructs.iam.permissions.IamPermission;
import com.aws.samples.cdk.constructs.iam.permissions.lambda.actions.ImmutableInvoke;
import com.aws.samples.cdk.constructs.iam.permissions.lambda.resources.ImmutableFunction;
import com.aws.samples.cdk.constructs.iam.policies.*;
import com.aws.samples.cdk.helpers.CdkHelper;
import com.aws.samples.cdk.stacktypes.JavaGradleStack;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vertx.core.impl.TaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static io.vertx.fargate.Helper.AUTHENTICATION_FUNCTION_LIST;
import static io.vertx.fargate.Helper.AUTHENTICATION_FUNCTION_LIST_ENVIRONMENT_VARIABLE;

public class IotCoreProxyStack extends software.amazon.awscdk.core.Stack implements JavaGradleStack {
    public static final String AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE = "AWS_ACCOUNT_ID";
    public static final String ROLE_TO_ASSUME_ENVIRONMENT_VARIABLE = "RoleToAssume";
    private static final String HANDLER_PACKAGE = "com.awssamples.iot.mqtt.auth.handlers";
    private static final String ANY_CLIENT_AUTH_HANDLER = String.join(".", HANDLER_PACKAGE, "AnyClientAuthHandler");
    private static final String HANDLE_REQUEST = "handleRequest";
    private static final Logger log = LoggerFactory.getLogger(IotCoreProxyStack.class);
    private static final String projectDirectory = "./";
    private static final String outputJar = "vertx.jar";
    private static final Path dockerfile = Paths.get("Dockerfile.nonTls");
    public static final String STACK_NAME_ENVIRONMENT_VARIABLE = "StackName";
    private static final Protocol elbv2Tcp = Protocol.TCP;
    private static final software.amazon.awscdk.services.ecs.Protocol ecsTcp = software.amazon.awscdk.services.ecs.Protocol.TCP;
    public static final Option<String> stackNameOption = Option.of("iot-core-proxy-stack");

    public static void main(String[] args) {
        stackNameOption.forEach(CdkHelper::setStackName);
        new IotCoreProxyStack(CdkHelper.getApp(), CdkHelper.getStackName());

        CdkHelper.getApp().synth();
    }

    public IotCoreProxyStack(final Construct parent, final String name) {
        super(parent, name);

        // Build all of the necessary JARs
        build();

        Vpc vpc = createVpc();

        Cluster cluster = createEcsCluster(vpc);

        Role fargateTaskRole = getFargateTaskRole();

        Role iotPublishRole = getIotPublishRole(fargateTaskRole);

        FargateTaskDefinition fargateTaskDefinition = getFargateTaskDefinition(fargateTaskRole);

        AssetImageProps assetImageProps = AssetImageProps.builder()
                .file(dockerfile.toString())
                .build();
        ContainerImage containerImage = ContainerImage.fromAsset(projectDirectory, assetImageProps);

        if (AUTHENTICATION_FUNCTION_LIST.size() == 0) {
            log.warn("No authentication functions specified, allowing any client to connect");
            Role anyClientAuthRole = buildAnyClientAuthRole();
            Function anyClientAuthFunction = buildAnyClientAuthFunction(anyClientAuthRole);
            AUTHENTICATION_FUNCTION_LIST = AUTHENTICATION_FUNCTION_LIST.append(anyClientAuthFunction.getFunctionName());

            ImmutableFunction function = ImmutableFunction.builder().functionName(anyClientAuthFunction.getFunctionName()).build();
            fargateTaskRole.addToPrincipalPolicy(ImmutableInvoke.builder().function(function).build().getPolicyStatement());
        } else {
            // Use the CDK constructs library to convert the function names into policy statements
            AUTHENTICATION_FUNCTION_LIST
                    .map(functionName -> ImmutableFunction.builder().functionName(functionName).build())
                    .map(function -> ImmutableInvoke.builder().function(function).build())
                    .map(IamPermission::getPolicyStatement)
                    // Add all of the new policy statements
                    .forEach(fargateTaskRole::addToPrincipalPolicy);
        }

        ContainerDefinition containerDefinition = getContainerDefinition(fargateTaskDefinition, containerImage, iotPublishRole, AUTHENTICATION_FUNCTION_LIST);

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

    private Function buildAnyClientAuthFunction(Role role) {

        HashMap<String, String> environment = HashMap.of(AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE, getAccount());

        FunctionProps functionProps = FunctionProps.builder()
                .code( Code.fromAsset( "src/main/java/com/awssamples/iot/mqtt/auth/handlers/python" ) )
                .handler("mqtt_proxy_auth.handler")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .runtime(Runtime.PYTHON_3_9)
                .role(role)
                .environment(environment.toJavaMap())
                .build();

        return new Function(this, "AnyClientAuthPythonLambda", functionProps);
    }

    private Role buildAnyClientAuthRole() {
        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(Collections.singletonList(CloudWatchLogsPolicies.minimalCloudWatchEventsLoggingPolicy))
                .build();
        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        HashMap<String, PolicyDocument> policyDocuments = HashMap.of("root", policyDocument);

        RoleProps anyClientAuthRoleProps = RoleProps.builder()
                .assumedBy(LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL)
                .inlinePolicies(policyDocuments.toJavaMap())
                .build();

        return new Role(this, "AnyClientAuthRole", anyClientAuthRoleProps);
    }

    private Role getIotPublishRole(Role taskRole) {
        // For publish, subscribe, receive
        List<IManagedPolicy> managedPolicies = List.of(IotPolicies.AWS_IOT_DATA_ACCESS_POLICY);

        RoleProps roleProps = RoleProps.builder()
                .managedPolicies(managedPolicies.asJava())
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

        List<IManagedPolicy> managedPolicies = List.of(
                // For describe endpoint
                IotPolicies.AWS_IOT_CONFIG_READ_ONLY_ACCESS_POLICY,
                // For CloudWatch metrics
                CloudWatchPolicies.CLOUD_WATCH_FULL_ACCESS_POLICY);

        HashMap<String, PolicyDocument> inlinePolicies = HashMap.of("AssumeRole", policyDocument);

        RoleProps roleProps = RoleProps.builder()
                .managedPolicies(managedPolicies.asJava())
                .inlinePolicies(inlinePolicies.toJavaMap())
                .assumedBy(EcsTaskPolicies.ECS_TASK_PRINCIPAL)
                .build();
        return new Role(this, "TaskRole", roleProps);
    }

    private NetworkTargetGroup getNetworkTargetGroup(HealthCheck healthCheck, NetworkListener networkListener, FargateService fargateService) {
        AddNetworkTargetsProps addNetworkTargetsProps = AddNetworkTargetsProps.builder()
                .deregistrationDelay(Duration.seconds(15))
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

        HashMap<String, String> environment = HashMap.of(
                ROLE_TO_ASSUME_ENVIRONMENT_VARIABLE, iotRole.getRoleName(),
                AUTHENTICATION_FUNCTION_LIST_ENVIRONMENT_VARIABLE, String.join(",", authenticationFunctionList),
                AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE, getAccount(),
                STACK_NAME_ENVIRONMENT_VARIABLE, getStackName());

        ContainerDefinitionProps containerDefinitionProps = ContainerDefinitionProps.builder()
                .taskDefinition(fargateTaskDefinition)
                .image(containerImage)
                .environment(environment.toJavaMap())
                .logging(logDriver)
                .build();

        return fargateTaskDefinition.addContainer("Container", containerDefinitionProps);
    }

    private FargateTaskDefinition getFargateTaskDefinition(Role taskRole) {
        FargateTaskDefinitionProps.Builder fargateTaskDefinitionPropsBuilder = FargateHelper.getValidMemoryAndCpu(FargateHelper.VCPU.One, 1024);
        FargateTaskDefinitionProps fargateTaskDefinitionProps = fargateTaskDefinitionPropsBuilder
                .taskRole(taskRole)
                .build();

        return new FargateTaskDefinition(this, "FargateTaskDefinition", fargateTaskDefinitionProps);
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

    @Override
    public String getProjectDirectory() {
        return projectDirectory;
    }

    @Override
    public String getOutputArtifactName() {
        return outputJar;
    }
}
