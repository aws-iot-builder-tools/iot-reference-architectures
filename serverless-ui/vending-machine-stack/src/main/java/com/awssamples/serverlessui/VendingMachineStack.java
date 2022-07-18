package com.awssamples.serverlessui;

import com.aws.samples.cdk.constructs.iam.permissions.SharedPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotActions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotResources;
import com.aws.samples.cdk.constructs.iam.permissions.iot.dataplane.actions.Connect;
import com.aws.samples.cdk.constructs.iam.permissions.iot.dataplane.actions.Publish;
import com.aws.samples.cdk.constructs.iam.permissions.iot.dataplane.actions.Receive;
import com.aws.samples.cdk.constructs.iam.permissions.iot.dataplane.actions.Subscribe;
import com.aws.samples.cdk.helpers.CdkHelper;
import com.aws.samples.cdk.helpers.IotHelper;
import com.aws.samples.cdk.helpers.RoleHelper;
import com.aws.samples.cdk.helpers.ServerlessHelper;
import com.aws.samples.cdk.helpers.data.AwsLambdaServlet;
import com.aws.samples.cdk.stacktypes.JavaGradleStack;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iot.CfnAuthorizer;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.aws.samples.cdk.constructs.iam.permissions.SharedPermissions.getAllowAllPolicyStatement;
import static com.aws.samples.cdk.helpers.ServerlessHelper.AUTHORIZERS;
import static java.util.Collections.singletonList;

public class VendingMachineStack extends software.amazon.awscdk.core.Stack implements JavaGradleStack {
    public static final String OWNER_UID = "993";
    public static final String OWNER_GID = "990";
    public static final String DOCKER_FUNCTION_NAME = "DOCKER_FUNCTION_NAME";
    private static final Logger log = LoggerFactory.getLogger(VendingMachineStack.class);
    public static final String VENDING_MACHINE_STACK = "vending-machine-stack";
    private final String projectDirectory;
    private final String outputArtifactName;
    private final List<CfnAuthorizer> iotCustomAuthorizers;
    private HashMap<String, String> lambdaEnvironment = HashMap.empty();

    public static void main(String[] args) {
        new VendingMachineStack(CdkHelper.getApp(), CdkHelper.getStackName());

        CdkHelper.getApp().synth();
    }

    public VendingMachineStack(final Construct parent, final String name) {
        super(parent, name);

        projectDirectory = "./";
        outputArtifactName = name + "-all.jar";

        // Build any Docker containers
        List<DockerImageCode> dockerImageCodeList = buildDockerContainers();

        Option<String> dockerFunctionNameOption = Option.none();

        Bucket imageBucket = null;

        BucketProps bucketProps = BucketProps.builder()
                .autoDeleteObjects(true)
                .bucketName(String.join("-", VENDING_MACHINE_STACK, "image-bucket"))
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        imageBucket = new Bucket(this, "image-bucket", bucketProps);

        Role noPermissionSsmRole = RoleHelper.buildRoleAssumedBySystemsManager(this,
                "no-permission-role-for-ssm",
                List.empty(),
                List.of(
                        ManagedPolicy.fromManagedPolicyArn(this, "ssm-managed-instance-core-policy", "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore")
                ));

        Role dockerLambdaRole = RoleHelper.buildRoleAssumedByLambda(this,
                "docker-lambda",
                List.empty(),
                List.of(
                        // Only for testing!
                        // ManagedPolicy.fromManagedPolicyArn(this, "admin-policy", "arn:aws:iam::aws:policy/AdministratorAccess")
                        ManagedPolicy.fromManagedPolicyArn(this, "lambda-basic-policy", "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"),
                        ManagedPolicy.fromManagedPolicyArn(this, "iot-full-access", "arn:aws:iam::aws:policy/AWSIoTFullAccess")
                ));

        PolicyStatementProps getRolePassRoleStatementProps = PolicyStatementProps.builder()
                .actions(List.of("iam:GetRole", "iam:PassRole").asJava())
                .resources(List.of(noPermissionSsmRole.getRoleArn()).asJava())
                .build();
        PolicyStatement getRolePassRoleStatement = new PolicyStatement(getRolePassRoleStatementProps);
        dockerLambdaRole.addToPolicy(getRolePassRoleStatement);

        PolicyStatementProps createActivationStatementProps = PolicyStatementProps.builder()
                .actions(List.of("ssm:CreateActivation").asJava())
                .resources(List.of(noPermissionSsmRole.getRoleArn()).asJava())
                .build();
        PolicyStatement createActivationStatement = new PolicyStatement(createActivationStatementProps);

        dockerLambdaRole.addToPolicy(createActivationStatement);

        PolicyStatementProps s3ListBucketProps = PolicyStatementProps.builder()
                .actions(List.of("s3:ListBucket").asJava())
                .resources(List.of(imageBucket.getBucketArn()).asJava())
                .build();

        PolicyStatement s3ListBucketStatement = new PolicyStatement(s3ListBucketProps);

        dockerLambdaRole.addToPolicy(s3ListBucketStatement);

        PolicyStatementProps createThingAndGroupProps = PolicyStatementProps.builder()
                .actions(List.of("iot:CreateThing", "iot:CreateThingGroup", "iot:UpdateThing", "iot:AddThingToThingGroup").asJava())
                .resources(List.of(SharedPermissions.ALL_RESOURCES).asJava())
                .build();

        PolicyStatement createThingAndGroupStatement = new PolicyStatement(createThingAndGroupProps);

        dockerLambdaRole.addToPolicy(createThingAndGroupStatement);

        String s3ResourceArn = String.join("/", imageBucket.getBucketArn(), "*");

        PolicyStatementProps s3PutGetObjectProps = PolicyStatementProps.builder()
                .actions(List.of("s3:PutObject", "s3:GetObject").asJava())
                .resources(List.of(s3ResourceArn).asJava())
                .build();
        PolicyStatement s3PutObjectStatement = new PolicyStatement(s3PutGetObjectProps);

        dockerLambdaRole.addToPolicy(s3PutObjectStatement);

        dockerLambdaRole.addToPolicy(getAllowAllPolicyStatement("s3:GetBucketLocation"));

        HashMap<String, String> dockerEnvironment = HashMap.of(
                "ssmRoleName", noPermissionSsmRole.getRoleName(),
                "s3Bucket", imageBucket.getBucketName()
        );

        DockerImageFunction dockerImageFunction = buildDockerLambda(this,
                "vending-machine",
                dockerLambdaRole,
                dockerEnvironment,
                dockerImageCodeList.get(),
                Option.none());

        dockerFunctionNameOption = Option.of(dockerImageFunction.getFunctionName());

        LambdaRestApi.Builder.create(this, "vending-machine-lambda-restapi")
                .restApiName("vending-machine")
                .description("Image builder")
                .endpointConfiguration(EndpointConfiguration.builder().types(singletonList(EndpointType.REGIONAL)).build())
                // NOTE: Do not remove this or GWT-RPC will not work!
                .binaryMediaTypes(singletonList("*/*"))
                .handler(dockerImageFunction)
                .build();

        // Build all of the necessary JARs
        String hash = build();

        // Optional - for custom authorizer demos
        iotCustomAuthorizers = IotHelper.getIotCustomAuthorizers(this, getOutputArtifactFile(), hash);
        logCount("IoT custom authorizers", iotCustomAuthorizers);

        addAuthorizerNamesToLambdaEnvironment();
        dockerFunctionNameOption.forEach(this::addDockerFunctionNameToLambdaEnvironment);

        // Build the properties required for the servlets
        FunctionProps.Builder lambdaFunctionPropsBuilder = FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .memorySize(1024)
                .environment(lambdaEnvironment.toJavaMap())
                // Can't go higher than 30 seconds because that's API gateway's max timeout
                .timeout(Duration.seconds(30));

        List<AwsLambdaServlet> awsLambdaServlets = ServerlessHelper.getAwsLambdaServlets(this, getOutputArtifactFile(), Option.of(lambdaFunctionPropsBuilder));
        logCount("servlets", awsLambdaServlets);

        List<Function> functions = awsLambdaServlets
                .map(servlet -> servlet.function);

        AtomicInteger atomicInteger = new AtomicInteger(0);

        // Build a role that can be assumed by each function
        // We can't access the role name here and filter on it since it is a CDK token, so this is a workaround
        List<Tuple2<Function, Role>> functionsAndRoles = functions
                .map(functionAndRole -> Tuple.of(functionAndRole,
                        RoleHelper.buildRoleAssumedByPrincipal(this,
                                "ssm-start-session-role-" + atomicInteger.getAndIncrement(),
                                List.empty(),
                                List.empty(),
                                functionAndRole.getRole())));

        functionsAndRoles.forEach(a -> log.info("function and role: " + a));

        // NOTE: This allows any session to be started but the Lambda function will scope it down to only the exact instance the user has access to
        PolicyStatementProps ssmStartSessionStatementProps = PolicyStatementProps.builder()
                .actions(List.of("ssm:StartSession").asJava())
                .resources(List.of("*").asJava())
                .build();

        PolicyStatement ssmStartSessionPolicyStatement = new PolicyStatement(ssmStartSessionStatementProps);

        // NOTE: This allows access to all the IoT resources related to this application but the Lambda function will scope it down
        String delimiter = "/";
        String topicPrefix = String.join(delimiter, "clients", "vendingmachine");
        String topicWildcard = String.join(delimiter, topicPrefix, SharedPermissions.ALL_RESOURCES);
        Publish publish = IotActions.publish(IotResources.topic(topicWildcard));
        Subscribe subscribe = IotActions.subscribe(IotResources.topicFilter(topicWildcard));
        Receive receive = IotActions.receive(IotResources.topic(topicWildcard));
        Connect connect = IotActions.connect(IotResources.clientId(SharedPermissions.ALL_RESOURCES));

        List<PolicyStatement> iotPolicyStatements = List.of(
                publish.getPolicyStatement(),
                subscribe.getPolicyStatement(),
                receive.getPolicyStatement(),
                connect.getPolicyStatement());

        // Give each role the IoT privileges
        iotPolicyStatements.forEach(policyStatement ->
                functionsAndRoles.map(tuple -> tuple._2)
                        .forEach(role -> role.addToPolicy(policyStatement)));

        // Give each role the start session privilege
        functionsAndRoles.map(tuple -> tuple._2)
                .forEach(role -> role.addToPolicy(ssmStartSessionPolicyStatement));

        // Allow each role to be assumed by anyone in the account
        functionsAndRoles.forEach(tuple -> tuple._1.getRole().grant(tuple._2, "sts:AssumeRole"));

        // Give each function the role ARN
        functionsAndRoles.forEach(tuple -> tuple._1.addEnvironment("roleToAssume", tuple._2.getRoleArn()));

        // Give each function the S3 bucket name
        Bucket finalImageBucket = imageBucket;
        functionsAndRoles.forEach(tuple -> tuple._1.addEnvironment("s3Bucket", finalImageBucket.getBucketName()));

        // Allow each function to access the S3 bucket
        functionsAndRoles.forEach(tuple -> finalImageBucket.grantRead(tuple._1.getRole()));

        LambdaRestApi lambdaRestApi = ServerlessHelper.buildLambdaRestApiIfPossible(this, awsLambdaServlets);

        // Include the URL for the serverless UI in the output so it's easier to find later
        CfnOutputProps url = CfnOutputProps.builder()
                .exportName(String.join("-", getStackName(), "url"))
                .value(lambdaRestApi.getUrl())
                .build();

        CfnOutput output = new CfnOutput(this, String.join("-", getStackName(), "api"), url);
    }

    private DockerImageFunction buildDockerLambda(Stack stack,
                                                  String functionNamePrefix,
                                                  Role role,
                                                  Map<String, String> defaultEnvironment,
                                                  DockerImageCode dockerImageCode,
                                                  Option<DockerImageFunctionProps.Builder> dockerImageFunctionPropsBuilderOption) {
        final DockerImageFunctionProps.Builder dockerImageFunctionPropsBuilder = DockerImageFunctionProps.builder()
                .environment(defaultEnvironment.toJavaMap())
                .timeout(Duration.seconds(600))
                .ephemeralStorageSize(Size.gibibytes(10))
                .memorySize(10240);

        // Populate the rest of the values that all functions need
        DockerImageFunctionProps dockerImageFunctionProps = getExistingOptionsIfNecessary(defaultEnvironment, dockerImageCode, dockerImageFunctionPropsBuilderOption, dockerImageFunctionPropsBuilder)
                .code(dockerImageCode)
                .role(role)
                .tracing(Tracing.ACTIVE)
                .build();

        return new DockerImageFunction(stack, String.join("-", functionNamePrefix, "docker", "lambda"), dockerImageFunctionProps);
    }

    private DockerImageFunctionProps.Builder getExistingOptionsIfNecessary(Map<String, String> defaultEnvironment, DockerImageCode dockerImageCode, Option<DockerImageFunctionProps.Builder> dockerImageFunctionPropsBuilderOption, DockerImageFunctionProps.Builder dockerImageFunctionPropsBuilder) {
        if (dockerImageFunctionPropsBuilderOption.isDefined()) {
            DockerImageFunctionProps.Builder tempDockerImageFunctionPropsBuilder = dockerImageFunctionPropsBuilderOption.get()
                    // Add a dummy code value to prevent the builder from failing
                    .code(dockerImageCode);
            DockerImageFunctionProps existingDockerImageFunctionProps = tempDockerImageFunctionPropsBuilder.build();

            // Sanity checks (the build() method did not check for these when this code was written)
            Option.of(existingDockerImageFunctionProps.getTimeout()).getOrElseThrow(() -> new RuntimeException("Existing Docker Lambda function props did not specify a timeout"));
            Option.of(existingDockerImageFunctionProps.getMemorySize()).getOrElseThrow(() -> new RuntimeException("Existing Docker Lambda function props did not specify a memory size"));

            // Get the existing environment
            HashMap<String, String> existingEnvironment = Option.of(existingDockerImageFunctionProps.getEnvironment())
                    // Convert it to a vavr HashMap
                    .map(HashMap::ofAll)
                    // Use an empty map if no environment was specified
                    .getOrElse(HashMap::empty);

            Map<String, String> environment = defaultEnvironment.merge(existingEnvironment);

            // Replace the empty builder with our existing builder and populate the environment
            dockerImageFunctionPropsBuilder = dockerImageFunctionPropsBuilderOption.get()
                    .environment(environment.toJavaMap());
        }
        return dockerImageFunctionPropsBuilder;
    }

    private List<DockerImageCode> buildDockerContainers() {
        File containersDirectory = new File(projectDirectory).toPath().resolve("containers").toFile();

        return Stream.of(Option.of(containersDirectory.listFiles()).getOrElse(new File[0]))
                // Ignore dot files
                .filter(c -> !c.getName().startsWith("."))
                // Get the files in each directory
                .map(File::listFiles)
                .flatMap(Stream::of)
                .filter(file -> file.getName().equals("Dockerfile"))
                .map(File::getParentFile)
                .map(file -> DockerImageCode.fromImageAsset(file.getAbsolutePath()))
                .toList();
    }

    private void logCount(String type, List<?> list) {
        if (list.isEmpty()) {
            log.info("No " + type + " found");
        } else {
            log.info("Number of " + type + " found: " + list.size());
        }
    }

    private void addAuthorizerNamesToLambdaEnvironment() {
        String authorizers = iotCustomAuthorizers
                .map(CfnAuthorizer::getAuthorizerName)
                .collect(Collectors.joining(","));

        if (!authorizers.isEmpty()) {
            lambdaEnvironment = lambdaEnvironment.put(AUTHORIZERS, authorizers);
        }
    }

    private void addDockerFunctionNameToLambdaEnvironment(String dockerFunctionName) {
        lambdaEnvironment = lambdaEnvironment.put(DOCKER_FUNCTION_NAME, dockerFunctionName);
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
