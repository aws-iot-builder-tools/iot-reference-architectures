package lambda;

import com.awslabs.general.helpers.implementations.JacksonHelper;
import io.vavr.Lazy;
import io.vavr.collection.HashMap;
import io.vavr.control.Try;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Logger;

public class SharedHelpers {
    public static final String ACTIVATION_ID = "activationId";
    public static final String STARTED = "started";
    public static final String VENDINGMACHINE = "vendingmachine";
    public static final String CLIENTS = "clients";
    public static final String FINISHED = "finished";
    private static final Logger log = Logger.getLogger(App.class.getName());
    private static final String TOPIC_PREFIX = String.join("/", CLIENTS, VENDINGMACHINE);
    private static final DescribeEndpointRequest DESCRIBE_ENDPOINT_REQUEST = DescribeEndpointRequest.builder().endpointType("iot:Data-ATS").build();
    private static final Lazy<IotClient> IOT_CLIENT = Lazy.of(IotClient::create);
    private static final Lazy<String> ENDPOINT_ADDRESS = Lazy.of(() -> IOT_CLIENT.get().describeEndpoint(DESCRIBE_ENDPOINT_REQUEST).endpointAddress());
    private static final Lazy<URI> ENDPOINT_URI = Lazy.of(() -> Try.of(() -> new URI(String.join("://", "https", ENDPOINT_ADDRESS.get()))).get());
    private static final Lazy<IotDataPlaneClient> IOT_DATA_PLANE_CLIENT = Lazy.of(() -> IotDataPlaneClient.builder().endpointOverride(ENDPOINT_URI.get()).build());
    private static int totalSteps = -1;
    private static int currentStep = 0;
    private static String buildId;
    private static String clientId;
    private static String comment;
    private static String userId;

    public static void publishBuildStarted() {
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(String.join("/", TOPIC_PREFIX, clientId, STARTED))
                .payload(SdkBytes.fromUtf8String(JacksonHelper.tryToJsonString(
                        HashMap.of(
                                STARTED, true,
                                "buildId", buildId)
                ).get()))
                .build();
        IOT_DATA_PLANE_CLIENT.get().publish(publishRequest);
    }

    public static void setTotalSteps(int totalSteps) {
        SharedHelpers.totalSteps = totalSteps;
    }

    public static String getBuildId() {
        return buildId;
    }

    public static void setBuildId(String buildId) {
        SharedHelpers.buildId = buildId;
    }

    public static void setClientId(String clientId) {
        SharedHelpers.clientId = clientId;
    }

    public static void setUserId(String userId) {
        SharedHelpers.userId = userId;
    }

    public static void startNextStep(String comment) {
        SharedHelpers.comment = comment;
        SharedHelpers.currentStep += 1;
        publishBuildProgress(0);
    }

    public static void finishStep() {
        publishBuildProgress(100);
    }

    public static void publishBuildProgress(int stepProgress) {
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(String.join("/", TOPIC_PREFIX, clientId, "progress"))
                .payload(SdkBytes.fromUtf8String(JacksonHelper.tryToJsonString(
                        HashMap.of("totalSteps", totalSteps,
                                "currentStep", currentStep,
                                "stepProgress", stepProgress,
                                "comment", comment,
                                "buildId", buildId)
                ).get()))
                .build();
        IOT_DATA_PLANE_CLIENT.get().publish(publishRequest);
    }

    public static void publishNewSystem(String activationId) {
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(String.join("/", TOPIC_PREFIX, clientId, "system"))
                .payload(SdkBytes.fromUtf8String(JacksonHelper.tryToJsonString(
                        HashMap.of(ACTIVATION_ID, activationId)
                ).get()))
                .build();
        IOT_DATA_PLANE_CLIENT.get().publish(publishRequest);
    }

    public static void createNewBuildInRegistry() {
        CreateThingGroupRequest createThingGroupRequest = CreateThingGroupRequest.builder()
                .thingGroupName(getThingGroupName())
                .build();

        IOT_CLIENT.get().createThingGroup(createThingGroupRequest);

        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(getThingName())
                .build();

        Try<CreateThingResponse> tryCreateThing = Try.of(() -> IOT_CLIENT.get().createThing(createThingRequest));

        if (tryCreateThing.isFailure()) {

        }

        AddThingToThingGroupRequest addThingToThingGroupRequest = AddThingToThingGroupRequest.builder()
                .thingGroupName(getThingGroupName())
                .thingName(getThingName())
                .build();

        IOT_CLIENT.get().addThingToThingGroup(addThingToThingGroupRequest);
    }

    private static String getThingGroupName() {
        return userId;
    }

    public static void addAttributeToBuildInRegistry(String key, String value) {
        AttributePayload attributePayload = AttributePayload.builder()
                .attributes(HashMap.of(
                        key, value
                ).toJavaMap())
                .merge(true)
                .build();

        UpdateThingRequest updateThingRequest = UpdateThingRequest.builder()
                .attributePayload(attributePayload)
                .thingName(getThingName())
                .build();

        IOT_CLIENT.get().updateThing(updateThingRequest);
    }

    private static String getThingName() {
        return buildId;
    }

    public static void publishBuildFinished(URL presignedS3Url) {
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(String.join("/", TOPIC_PREFIX, clientId, FINISHED))
                .payload(SdkBytes.fromUtf8String(JacksonHelper.tryToJsonString(HashMap.of(
                        FINISHED, presignedS3Url.toString(),
                        "buildId", buildId)
                ).get()))
                .build();
        IOT_DATA_PLANE_CLIENT.get().publish(publishRequest);
    }

    public static String sha256Hash(String inputString) {
        return Try.of(() -> MessageDigest.getInstance("SHA-256"))
                .map(messageDigest -> messageDigest.digest(inputString.getBytes(StandardCharsets.UTF_8)))
                .map(SharedHelpers::bytesToHex)
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

    public static void info(String message) {
        log.info(message);
        IOT_DATA_PLANE_CLIENT.get().publish(PublishRequest.builder().topic("info").payload(SdkBytes.fromString(message, StandardCharsets.UTF_8)).build());
    }

    public static void warning(String message) {
        log.warning(message);
        IOT_DATA_PLANE_CLIENT.get().publish(PublishRequest.builder().topic("warning").payload(SdkBytes.fromString(message, StandardCharsets.UTF_8)).build());
    }

    public static void resetCurrentStep() {
        currentStep = 0;
    }
}
