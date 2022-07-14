package awslabs.server;

import awslabs.client.IotService;
import awslabs.client.mqtt.ClientConfig;
import awslabs.client.shared.IotBuild;
import awslabs.client.shared.IotSystem;
import awslabs.client.shared.RaspberryPiRequest;
import awslabs.client.ssm.SsmConfig;
import com.aws.samples.cdk.constructs.iam.permissions.HasIamPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.IamPermission;
import com.aws.samples.cdk.constructs.iam.permissions.SharedPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotActions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotResources;
import com.aws.samples.cdk.constructs.iam.permissions.lambda.LambdaActions;
import com.aws.samples.cdk.constructs.iam.permissions.ssm.SsmActions;
import com.aws.samples.lambda.servlet.LambdaWebServlet;
import com.awslabs.cloudformation.data.ImmutableStackName;
import com.awslabs.cloudformation.data.StackName;
import com.awslabs.cloudformation.interfaces.CloudFormationHelper;
import com.awslabs.general.helpers.implementations.JacksonHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.awslabs.resultsiterator.implementations.ResultsIteratorAbstract;
import com.awslabs.s3.helpers.data.ImmutableS3Bucket;
import com.awslabs.s3.helpers.data.ImmutableS3Key;
import com.awslabs.s3.helpers.data.S3Bucket;
import com.awslabs.s3.helpers.data.S3Key;
import com.awslabs.s3.helpers.interfaces.S3Helper;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.SearchIndexRequest;
import software.amazon.awssdk.services.iot.model.ThingDocument;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static awslabs.client.SharedWithServer.topicPrefix;
import static software.amazon.awssdk.services.ssm.model.InstanceInformationFilterKey.ACTIVATION_IDS;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings({"serial", "GwtServiceNotRegistered"})
@WebServlet(name = "IotService", displayName = "BasicIotService", urlPatterns = {"/app/iot"}, loadOnStartup = 1)
@LambdaWebServlet
public class BasicIotService extends RemoteServiceServlet implements IotService, HasIamPermissions {
    public static final String DELIMITER = "/";
    public static final String HASH_KEY = "hash";
    public static final String BODY_KEY = "body";
    public static final String ROLE_TO_ASSUME = "roleToAssume";
    public static final String S3_BUCKET = "s3Bucket";
    // This must be lazy so it doesn't blow up when the code generation tries to instantiate the class and get its permissions
    public static final Lazy<S3Bucket> lazyS3Bucket = Lazy.of(() -> ImmutableS3Bucket.builder()
            .bucket(Option.of(System.getenv(S3_BUCKET))
                    .getOrElseThrow(() -> new RuntimeException(S3_BUCKET + " environment variable not present, cannot continue")))
            .build());
    private static final String endpointAddress = IotClient.create().describeEndpoint(r -> r.endpointType("iot:Data-ATS")).endpointAddress();
    private static final StsClient stsClient = StsClient.create();
    private static final Logger log = LoggerFactory.getLogger(BasicIotService.class);
    // This must be lazy so it doesn't blow up when the code generation tries to instantiate the class and get its permissions
    private static final Lazy<String> roleToAssumeLazy = Lazy.of(BasicIotService::roleToAssumeEager);
    private static final Lazy<Injector> lazyInjector = Lazy.of(DaggerInjector::create);
    private static final String region = DefaultAwsRegionProviderChain.builder().build().getRegion().id();
    // To find resources via CloudFormation APIs for local debugging
    private final StackName stackName = ImmutableStackName.builder().stackName("vending-machine-stack").build();

    @Inject
    S3Helper s3Helper;
    @Inject
    IotClient iotClient;
    @Inject
    CloudFormationHelper cloudFormationHelper;

    private static String roleToAssumeEager() {
        return Option.of(System.getenv(ROLE_TO_ASSUME))
                .getOrElseThrow(() -> new RuntimeException("Role to assume [" + ROLE_TO_ASSUME + "] environment variable not present, cannot continue"));
    }

    private static String sha256Hash(String inputString) {
        return Try.of(() -> MessageDigest.getInstance("SHA-256"))
                .map(messageDigest -> messageDigest.digest(inputString.getBytes(StandardCharsets.UTF_8)))
                .map(BasicIotService::bytesToHex)
                .get();
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);

            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    @Override
    public ClientConfig getClientConfig(String userIdNullable) {
        String userId = Option.of(userIdNullable)
                .getOrElse(() -> UUID.randomUUID().toString().substring(0, 8));

        String hashedUserId = sha256Hash(userId);

        Credentials credentials;

        if (!SharedPermissions.isRunningInLambda()) {
            // Running locally, get session token
            credentials = stsClient.getSessionToken().credentials();
        } else {
            // Running in Lambda, assume a role
            String iamPrefix = "arn:aws:iot:" + new DefaultAwsRegionProviderChain().getRegion().id() + ":" + stsClient.getCallerIdentity().account() + ":";
            String topic = iamPrefix + String.join(DELIMITER, "topic", topicPrefix, hashedUserId, SharedPermissions.ALL_RESOURCES);
            String topicFilter = iamPrefix + String.join(DELIMITER, "topicfilter", topicPrefix, hashedUserId, SharedPermissions.ALL_RESOURCES);
            String client = iamPrefix + String.join(DELIMITER, "client", hashedUserId);
            HashMap<String, String> publish = HashMap.of(
                    "Effect", "Allow",
                    "Action", "iot:Publish",
                    "Resource", topic);
            HashMap<String, String> subscribe = HashMap.of(
                    "Effect", "Allow",
                    "Action", "iot:Subscribe",
                    "Resource", topicFilter);
            HashMap<String, String> receive = HashMap.of(
                    "Effect", "Allow",
                    "Action", "iot:Receive",
                    "Resource", topic);
            HashMap<String, String> connect = HashMap.of(
                    "Effect", "Allow",
                    "Action", "iot:Connect",
                    "Resource", client);
            HashMap<String, Object> policyMap = HashMap.of(
                    "Version", "2012-10-17",
                    "Statement", io.vavr.collection.List.of(publish, subscribe, receive, connect)
            );

            String policyJson = JacksonHelper.tryToJsonString(policyMap).get();
            log.info("policyJson: " + policyJson);

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .durationSeconds(1800)
                    .roleSessionName(UUID.randomUUID().toString().replaceAll("-", ""))
                    .roleArn(roleToAssumeLazy.get())
                    .policy(policyJson)
                    .build();

            AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
            credentials = assumeRoleResponse.credentials();
        }

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.accessKeyId = credentials.accessKeyId();
        clientConfig.secretAccessKey = credentials.secretAccessKey();
        clientConfig.sessionToken = credentials.sessionToken();
        clientConfig.endpointAddress = endpointAddress;
        clientConfig.region = DefaultAwsRegionProviderChain.builder().build().getRegion().toString();
        clientConfig.clientId = hashedUserId;
        clientConfig.userId = userId;

        return clientConfig;
    }

    @Override
    public List<IotBuild> getBuildList(String userId) {
        lazyInjector.get().inject(this);

        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString("thingGroupNames:" + userId)
                .build();

        // Converting to ArrayList is necessary since GWT-RPC can't serialize Vavr's ListView (which is what asJava() returns)
        return new ArrayList<>(
                new ResultsIteratorAbstract<ThingDocument>(iotClient, searchIndexRequest) {
                }.stream()
                        .map(ThingDocument::thingName)
                        .map(IotBuild::new)
                        // flatMap with Option::of deals with NULLs
                        .map(iotBuild -> iotBuild.buildAvailable(Try.of(() -> getPresignedS3Url(iotBuild.name())).toOption().flatMap(Option::of).isDefined()))
                        .toList()
                        .asJava());
    }

    @Override
    public List<IotSystem> getSystemList(String userId) {
        lazyInjector.get().inject(this);

        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString("thingGroupNames:" + userId)
                .build();

        Stream<ThingDocument> thingDocumentStream = new ResultsIteratorAbstract<ThingDocument>(iotClient, searchIndexRequest) {
        }.stream();

        // Converting to ArrayList is necessary since GWT-RPC can't serialize Vavr's ListView (which is what asJava() returns)
        return new ArrayList<>(
                thingDocumentStream
                        .filter(thingDocument -> thingDocument.attributes().containsKey("activationId"))
                        .map(thingDocument -> thingDocument.attributes().get("activationId"))
                        .map(activationId -> Tuple.of(nameFromActivationId(activationId), activationId))
                        .filter(tuple -> tuple._1.isDefined())
                        .map(tuple -> tuple.map1(Option::get))
                        .map(tuple -> new IotSystem(tuple._1, tuple._2))
                        .map(iotSystem -> iotSystem.online(Try.of(() -> isSystemOnline(iotSystem.activationId())).getOrElse(false)))
                        .toList()
                        .asJava());
    }

    @Override
    public String getPresignedS3Url(String buildName) {
        lazyInjector.get().inject(this);

        S3Bucket s3Bucket;

        if (!SharedPermissions.isRunningInLambda()) {
            // Running locally
            String bucketName = cloudFormationHelper.getStackResource(stackName, "AWS::S3::Bucket", Option.of("image-bucket"))
                    .getOrElseThrow(() -> new RuntimeException("Could not find image-bucket stack resource"));
            s3Bucket = ImmutableS3Bucket.builder().bucket(bucketName).build();
        } else {
            s3Bucket = lazyS3Bucket.get();
        }

        S3Key s3Key = ImmutableS3Key.builder().key(String.join(".", buildName, "img", "zip")).build();

        if (!s3Helper.objectExists(s3Bucket, s3Key)) {
            return null;
        }

        URL presignedS3Url = s3Helper.presign(s3Bucket, s3Key, Duration.ofMinutes(20));

        return presignedS3Url.toString();
    }

    @Override
    public String buildImage(RaspberryPiRequest raspberryPiRequest) {
        String dockerFunctionName;

        if (!SharedPermissions.isRunningInLambda()) {
            // Running locally
            dockerFunctionName = cloudFormationHelper.getStackResource(stackName, "AWS::Lambda::Function", Option.of("vending-machine-docker-lambda"))
                    .getOrElseThrow(() -> new RuntimeException("Could not find vending-machine-docker-lambda stack resource"));
        } else {
            Option<String> dockerFunctionNameOption = Option.of(System.getenv("DOCKER_FUNCTION_NAME"));

            if (dockerFunctionNameOption.isEmpty()) {
                throw new RuntimeException("Docker function name not found");
            }

            dockerFunctionName = dockerFunctionNameOption.get();
        }

        LambdaClient lambdaClient = LambdaClient.create();

        byte[] jsonBytes = JacksonHelper.tryToJsonBytes(raspberryPiRequest).get();
        String jsonString = new String(jsonBytes);
        log.info("JSON string before dry run invoke:" + jsonString);
        SdkBytes payload = SdkBytes.fromByteArray(jsonBytes);

        InvokeRequest syncInvokeRequest = InvokeRequest.builder()
                .functionName(dockerFunctionName)
                .payload(payload)
                .build();

        InvokeResponse response = lambdaClient.invoke(syncInvokeRequest);

        String hash = JacksonHelper.tryParseJson(response.payload().asUtf8String(), Map.class)
                .flatMap(outerMap -> JacksonHelper.tryParseJson((String) outerMap.get(BODY_KEY), Map.class))
                .filter(innerMap -> innerMap.containsKey(HASH_KEY))
                .map(innerMap -> innerMap.get(HASH_KEY))
                .map(String.class::cast)
                .get();

        log.info("Received hash: " + hash);

        raspberryPiRequest.dryRun = false;

        jsonBytes = JacksonHelper.tryToJsonBytes(raspberryPiRequest).get();
        jsonString = new String(jsonBytes);
        log.info("JSON string before final invoke:" + jsonString);
        payload = SdkBytes.fromByteArray(jsonBytes);

        InvokeRequest asyncInvokeRequest = InvokeRequest.builder()
                .functionName(dockerFunctionName)
                .payload(payload)
                .invocationType(InvocationType.EVENT)
                .build();

        lambdaClient.invoke(asyncInvokeRequest);

        return hash;
    }

    private boolean isSystemOnline(String activationId) {
        return !getInstanceInformationListForActivationId(activationId)
                .filter(instance -> instance.pingStatus().equals(PingStatus.ONLINE))
                .isEmpty();
    }

    private Option<String> nameFromActivationId(String activationId) {
        return getActivationForActivationId(activationId)
                .toOption()
                .map(Activation::defaultInstanceName);
    }

    private io.vavr.collection.List<Activation> getActivationForActivationId(String activationId) {
        SsmClient ssmClient = SsmClient.create();

        DescribeActivationsFilter describeActivationsFilter = DescribeActivationsFilter.builder()
                .filterKey(DescribeActivationsFilterKeys.ACTIVATION_IDS)
                .filterValues(activationId)
                .build();

        DescribeActivationsRequest describeActivationsRequest = DescribeActivationsRequest.builder()
                .filters(describeActivationsFilter)
                .build();

        ResultsIterator<Activation> resultsIterator = new ResultsIterator<>(ssmClient, describeActivationsRequest);

        return resultsIterator.stream().toList();
    }

    private io.vavr.collection.List<InstanceInformation> getInstanceInformationListForActivationId(String activationId) {
        SsmClient ssmClient = SsmClient.create();

        InstanceInformationFilter instanceInformationFilter = InstanceInformationFilter.builder()
                .key(ACTIVATION_IDS)
                .valueSet(activationId)
                .build();

        DescribeInstanceInformationRequest describeInstanceInformationRequest = DescribeInstanceInformationRequest.builder()
                .instanceInformationFilterList(instanceInformationFilter)
                .build();

        ResultsIterator<InstanceInformation> resultsIterator = new ResultsIterator<>(ssmClient, describeInstanceInformationRequest);

        return resultsIterator.stream().toList();
    }

    @Override
    public SsmConfig getSessionManagerConfig(String activationId) {
        return getInstanceIdOption(activationId)
                .map(this::getWebSocketUrlForTerminal)
                .getOrNull();
    }

    private Option<String> getInstanceIdOption(String activationId) {
        return getInstanceInformationListForActivationId(activationId)
                .toOption()
                .map(InstanceInformation::instanceId);
    }

    @Override
    public String getSessionManagerUrl(String activationId) {
        return getInstanceIdOption(activationId)
                .map(this::getSignInUrlForConsole)
                .getOrNull();
    }

    private SsmConfig getWebSocketUrlForTerminal(String instanceId) {
        Credentials assumedCredentials = getAssumedCredentialsToStartSessionToManagedInstance(instanceId, region);
        AwsSessionCredentials awsSessionCredentials = AwsSessionCredentials.create(assumedCredentials.accessKeyId(), assumedCredentials.secretAccessKey(), assumedCredentials.sessionToken());
        StaticCredentialsProvider staticCredentialsProvider = StaticCredentialsProvider.create(awsSessionCredentials);
        SsmClient ssmClient = SsmClient.builder().credentialsProvider(staticCredentialsProvider).build();

        StartSessionRequest startSessionRequest = StartSessionRequest.builder()
                .target(instanceId)
                .build();
        StartSessionResponse startSessionResponse = ssmClient.startSession(startSessionRequest);

        log.error("stream URL: " + startSessionResponse.streamUrl());
        log.error("session ID: " + startSessionResponse.sessionId());
        log.error("token value: " + startSessionResponse.tokenValue());

        return new SsmConfig(startSessionResponse.streamUrl(), startSessionResponse.tokenValue());
    }

    private String getSignInUrlForConsole(String instanceId) {
        Credentials assumedCredentials = getAssumedCredentialsToStartSessionToManagedInstance(instanceId, region);

        // The issuer parameter specifies your internal sign-in
        // page, for example https://mysignin.internal.mycompany.com/.
        // The console parameter specifies the URL to the destination console of the
        // AWS Management Console. This example goes to Amazon SNS.
        // The signin parameter is the URL to send the request to.

        String issuerURL = "https://mysignin.internal.mycompany.com/";
        //        String consoleURL = "https://console.aws.amazon.com/sns";
        String consoleURL = "https://" + region + ".console.aws.amazon.com/systems-manager/session-manager/" + instanceId + "?region=" + region + "#";
        String signInURL = "https://signin.aws.amazon.com/federation";

        // Create the sign-in token using temporary credentials,
        // including the access key ID,  secret access key, and security token.
        String sessionJson = String.format(
                "{\"%1$s\":\"%2$s\",\"%3$s\":\"%4$s\",\"%5$s\":\"%6$s\"}",
                "sessionId", assumedCredentials.accessKeyId(),
                "sessionKey", assumedCredentials.secretAccessKey(),
                "sessionToken", assumedCredentials.sessionToken());

        // Construct the sign-in request with the request sign-in token action, a
        // 12-hour console session duration, and the JSON document with temporary
        // credentials as parameters.

        String getSigninTokenURL = signInURL +
                "?Action=getSigninToken" +
                "&DurationSeconds=43200" +
                "&SessionType=json&Session=" +
                Try.of(() -> URLEncoder.encode(sessionJson, StandardCharsets.UTF_8)).get();

        URL url = Try.of(() -> new URL(getSigninTokenURL)).get();

        // Send the request to the AWS federation endpoint to get the sign-in token
        URLConnection conn = Try.of(url::openConnection).get();

        BufferedReader bufferReader = new BufferedReader(new
                InputStreamReader(Try.of(conn::getInputStream).get()));
        String returnContent = Try.of(bufferReader::readLine).get();

        String signinToken = JacksonHelper.tryParseJson(returnContent, Map.class)
                .map(map -> map.get("SigninToken"))
                .toOption()
                .map(String.class::cast)
                .getOrElseThrow(() -> new RuntimeException("Failed to parse return content [" + returnContent + "]"));

        String signinTokenParameter = "&SigninToken=" + Try.of(() -> URLEncoder.encode(signinToken, StandardCharsets.UTF_8)).get();

        // The issuer parameter is optional, but recommended. Use it to direct users
        // to your sign-in page when their session expires.

        String issuerParameter = "&Issuer=" + Try.of(() -> URLEncoder.encode(issuerURL, StandardCharsets.UTF_8)).get();

        // Finally, present the completed URL for the AWS console session to the user

        String destinationParameter = "&Destination=" + Try.of(() -> URLEncoder.encode(consoleURL, StandardCharsets.UTF_8)).get();
        String loginURL = signInURL + "?Action=login" +
                signinTokenParameter + issuerParameter + destinationParameter;

        return loginURL;
    }

    private Credentials getAssumedCredentialsToStartSessionToManagedInstance(String instanceId, String region) {
        StsClient stsClient = StsClient.create();

        String accountId = stsClient.getCallerIdentity().account();
        String resourceArn = String.join("/", "arn:aws:ssm:" + region + ":" + accountId + ":managed-instance", instanceId);
        HashMap<String, Serializable> policyMap = HashMap.of("Version", "2012-10-17",
                "Statement",
                io.vavr.collection.List.of(
                        HashMap.of("Effect", "Allow",
                                "Resource", io.vavr.collection.List.of(resourceArn),
                                "Action", io.vavr.collection.List.of("ssm:StartSession"))));

        String policy = JacksonHelper.tryToJsonString(policyMap).get();

        String roleToAssume;

        if (!SharedPermissions.isRunningInLambda()) {
            // Running locally
//            roleToAssume = getStackResource("AWS::IAM::Role", Option.of("ssmstartsessionrole"));
            return StsClient.create().getSessionToken().credentials();
        }

        roleToAssume = roleToAssumeLazy.get();

        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(1800)
                .roleSessionName(UUID.randomUUID().toString().replaceAll("-", ""))
                .policy(policy)
                .roleArn(roleToAssume)
                .build();

        return stsClient.assumeRole(assumeRoleRequest).credentials();
    }

    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL, String strongName) {
        if (!SharedPermissions.isRunningInLambda()) {
            log.info("Not in Lambda, using default serialization policy setup");
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } else {
            log.info("In Lambda, using custom serialization policy setup");
            log.info("BEFORE: " + moduleBaseURL);
            moduleBaseURL = moduleBaseURL.replace("/prod", "");
            log.info("AFTER: " + moduleBaseURL);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        }
    }

    @Override
    public io.vavr.collection.List<IamPermission> getPermissions() {
        return io.vavr.collection.List.of(
                IotActions.publish(IotResources.topic(String.join(DELIMITER, topicPrefix, SharedPermissions.ALL_RESOURCES))),
                IotActions.subscribe(IotResources.topicFilter(String.join(DELIMITER, topicPrefix, SharedPermissions.ALL_RESOURCES))),
                IotActions.receive(IotResources.topic(String.join(DELIMITER, topicPrefix, SharedPermissions.ALL_RESOURCES))),
                IotActions.connect(IotResources.clientId(SharedPermissions.ALL_RESOURCES)),
                IotActions.describeEndpoint,
                IotActions.searchIndex,
                SsmActions.describeInstanceInformation,
                SsmActions.describeActivations,
                LambdaActions.invokeAll
        );
    }
}
