package io.vertx.fargate.mqtt.handlers;

import com.awslabs.aws.iot.websockets.MqttOverWebsocketsProvider;
import com.awslabs.aws.iot.websockets.data.*;
import com.google.gson.Gson;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vavr.Lazy;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.TaskQueue;
import io.vertx.fargate.Helper;
import io.vertx.fargate.mqtt.data.ImmutableMqttClientAndEndpoint;
import io.vertx.fargate.mqtt.data.MqttClientAndEndpoint;
import io.vertx.fargate.mqtt.interfaces.MqttBackendHandler;
import io.vertx.fargate.mqtt.interfaces.MqttCallbackHandler;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.sts.model.StsException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.net.ssl.SSLSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vertx.fargate.Helper.AUTHENTICATION_FUNCTION_LIST;
import static io.vertx.fargate.modules.BaselineDaggerIotBrokerModule.AUTH_INVOCATION_COUNTER;

public class BasicMqttEndpointHandler implements MqttBackendHandler, Handler<MqttEndpoint> {
    public static final String ROLE_TO_ASSUME = "RoleToAssume";
    private static final String MAKE_SURE_THAT_THE_ROLE_CAN_BE_ASSUMED = "- Make sure that the role can be assumed by this system [ecs-tasks, EC2, etc]";
    private static final Logger log = LoggerFactory.getLogger(BasicMqttEndpointHandler.class);
    private static final Lazy<Dimension> stackNameDimensionLazy = Lazy.of(() -> Dimension.builder().name("Stack").value(Helper.stackNameLazy.get()).build());

    @Inject
    MqttOverWebsocketsProvider mqttOverWebsocketsProvider;
    @Inject
    List<MqttQoS> grantedQosLevels;
    @Inject
    Provider<MqttCallbackHandler> mqttCallbackHandlerProvider;
    Lazy<LambdaClient> lazyLambdaClient = Lazy.of(LambdaClient::create);
    Lazy<CloudWatchClient> lazyCloudWatchClient = Lazy.of(CloudWatchClient::create);
    @Inject
    Vertx vertx;
    private final Option<ImmutableRoleToAssume> roleToAssumeOption = getRoleToAssume();
    private final TaskQueue authTaskQueue = new TaskQueue();
    @Inject
    ExecutorService authExecutorService;
    @Inject
    @Named(AUTH_INVOCATION_COUNTER)
    AtomicInteger authInvocationCounter;

    @Inject
    public BasicMqttEndpointHandler() {
    }

    @Override
    public void handle(MqttEndpoint mqttEndpoint) {
        // Log relevant MQTT client connection info to info stream
        logConnection(mqttEndpoint);

        // Log relevant MQTT client connection info to debug stream
        debugLogConnection(mqttEndpoint);

        // Invoking the authentication functions blocks, use executeBlocking to not block the main thread
        vertx.executeBlocking(future -> getScopedDownMqttClient(mqttEndpoint, future),
                this::acceptConnectionAndSetUpCallbacks);
    }

    private void acceptConnectionAndSetUpCallbacks(AsyncResult<MqttClientAndEndpoint> result) {
        if (result.failed()) {
            log.warn("Connection failed, giving up [" + result.cause().getMessage() + "]");
            logFailureInCloudWatch();
            return;
        }

        try {
            Instant start = Instant.now();
            MqttClientAndEndpoint mqttClientAndEndpoint = result.result();

            MqttEndpoint mqttEndpoint = mqttClientAndEndpoint.getMqttEndpoint();
            MqttClient mqttClient = mqttClientAndEndpoint.getMqttClient();

            // Accept connection from the remote client.  We don't support sessions so we'll indicate there's no session present (false value).
            mqttEndpoint.accept(false);

            // Set up the handlers for messages from the client

            // Subscribe requests
            mqttEndpoint.subscribeHandler(subscribe -> subscribeAndAcknowledge(mqttEndpoint, mqttClient, subscribe));

            // Unsubscribe requests
            mqttEndpoint.unsubscribeHandler(unsubscribe -> unsubscribeAndAcknowledge(mqttEndpoint, mqttClient, unsubscribe));

            // Log client ping messages
            mqttEndpoint.pingHandler(v -> logPing());

            // Log client disconnect messages
            mqttEndpoint.disconnectHandler(v -> logDisconnect());

            // Log client closes and disconnect from AWS IoT
            mqttEndpoint.closeHandler(v -> close(mqttClient));

            // Publish requests (does not handle QoS1 for simplicity)
            mqttEndpoint.publishHandler(message -> logAndPublish(mqttEndpoint, mqttClient, message));

            // Send PUBCOMP
            mqttEndpoint.publishReleaseHandler(mqttEndpoint::publishComplete);

            // On all exceptions just disconnect and log it
            mqttEndpoint.exceptionHandler(throwable -> disconnectAndLog(throwable, mqttEndpoint));

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            logSuccessInCloudWatch(duration);
        } catch (Exception e) {
            logFailureInCloudWatch();
        }
    }

    private void getScopedDownMqttClient(MqttEndpoint mqttEndpoint, Promise<MqttClientAndEndpoint> promise) {
        authenticateAndGetScopeDownPolicy(mqttEndpoint, scopeDownPolicyOption -> {
            if (scopeDownPolicyOption.isEmpty()) {
                log.error("No scope down policy found for client, dropping connection.");
                mqttEndpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);
                mqttEndpoint.close();
                return;
            }

            MqttClient mqttClient = getMqttClient(mqttEndpoint, scopeDownPolicyOption);

            MqttClientAndEndpoint mqttClientAndEndpoint = ImmutableMqttClientAndEndpoint.builder()
                    .mqttClient(mqttClient)
                    .mqttEndpoint(mqttEndpoint)
                    .build();

            promise.complete(mqttClientAndEndpoint);
        });
    }

    private Void logFailureInCloudWatch() {
        putCountingMetric("FAILED_CONNECTION");
        return null;
    }

    private Void logSuccessInCloudWatch(Duration duration) {
        putCountingMetric("SUCCESSFUL_CONNECTION");
        putDurationMetric("SUCCESSFUL_CONNECTION_SETUP_DURATION_MS", duration.toMillis(), StandardUnit.MILLISECONDS);
        return null;
    }

    private void putCountingMetric(String metricName) {
        vertx.executeBlocking(promise -> {
            MetricDatum datum = MetricDatum.builder()
                    .metricName(metricName)
                    .unit(StandardUnit.COUNT)
                    .dimensions(stackNameDimensionLazy.get())
                    .value((double) 1)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace("IOT_PROXY")
                    .metricData(datum)
                    .build();

            Try.of(() -> lazyCloudWatchClient.get().putMetricData(request))
                    .onSuccess(response -> promise.complete())
                    .onFailure(promise::fail);
        });
    }

    private void putDurationMetric(String metricName, double duration, StandardUnit unit) {
        vertx.executeBlocking(promise -> {
            MetricDatum datum = MetricDatum.builder()
                    .metricName(metricName)
                    .unit(unit)
                    .dimensions(stackNameDimensionLazy.get())
                    .value(duration)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace("IOT_PROXY")
                    .metricData(datum)
                    .build();

            Try.of(() -> lazyCloudWatchClient.get().putMetricData(request))
                    .onSuccess(response -> promise.complete())
                    .onFailure(promise::fail);
        });
    }

    private MqttClient getMqttClient(MqttEndpoint mqttEndpoint, Option<String> scopeDownPolicyJsonOption) {
        // Get a new callback handler and assign this client to it.  This handles messages from AWS IoT.
        MqttCallbackHandler mqttCallbackHandler = mqttCallbackHandlerProvider.get();
        mqttCallbackHandler.setMqttEndpoint(mqttEndpoint);

        ClientId clientId = ImmutableClientId.builder().clientId(mqttEndpoint.clientIdentifier()).build();

        MqttOverWebsocketsUriConfig mqttOverWebsocketsUriConfig = ImmutableMqttOverWebsocketsUriConfig.builder()
                .optionalScopeDownPolicyJson(scopeDownPolicyJsonOption.toJavaOptional())
                .optionalRoleToAssume(roleToAssumeOption.toJavaOptional())
                .build();

        MqttClientConfig mqttClientConfig = ImmutableMqttClientConfig.builder()
                .clientId(clientId)
                .optionalMqttOverWebsocketsUriConfig(mqttOverWebsocketsUriConfig)
                .build();

        return Try.of(() -> createBackendConnection(mqttOverWebsocketsProvider, mqttCallbackHandler, mqttClientConfig))
                .onFailure(throwable -> disconnectAndLog(throwable, mqttEndpoint))
                .get();
    }

    private void debugLogConnection(MqttEndpoint mqttEndpoint) {
        if (mqttEndpoint.will() != null) {
            log.debug("[will flag = {}, topic = {}, msg = {}, QoS = {}, isRetain = {}]",
                    mqttEndpoint.will().isWillFlag(),
                    mqttEndpoint.will().getWillTopic(),
                    mqttEndpoint.will().getWillMessage(),
                    mqttEndpoint.will().getWillQos(),
                    mqttEndpoint.will().isWillRetain());
        }

        log.debug("[keep alive timeout = {}]",
                mqttEndpoint.keepAliveTimeSeconds());
    }

    private void logConnection(MqttEndpoint mqttEndpoint) {
        log.info("MQTT client [{}] request to connect, clean session = {}",
                mqttEndpoint.clientIdentifier(),
                mqttEndpoint.isCleanSession());
    }

    private void authenticateAndGetScopeDownPolicy(MqttEndpoint mqttEndpoint, Handler<Option<String>> resultHandler) {
        if (AUTHENTICATION_FUNCTION_LIST.size() == 0) {
            disconnectAndLog(new RuntimeException("No authentication functions were provided, can not continue"), mqttEndpoint);
            return;
        }

        disconnectAndLogOnFailure(Try.run(() -> queuedInvokeAuthenticationFunctions(mqttEndpoint, resultHandler)), mqttEndpoint);
    }

    private void queuedInvokeAuthenticationFunctions(MqttEndpoint mqttEndpoint, Handler<Option<String>> resultHandler) {
        // Combine all of the individual futures below into a single future with all of the results
        authTaskQueue.execute(() -> AUTHENTICATION_FUNCTION_LIST
                        .peek(v -> log.info("Auth function invocation #" + authInvocationCounter.incrementAndGet()))
                        // Asynchronously invoke all of the functions
                        .map(functionName -> syncInvokeFunction(functionName, mqttEndpoint))
                        // When we are done find the first defined value, then unwrap the double optional
                        .find(Option::isDefined)
                        .forEach(resultHandler::handle),
                authExecutorService);
    }

    private void asyncInvokeAuthenticationFunctions(MqttEndpoint mqttEndpoint, Handler<Option<String>> resultHandler) {
        // Combine all of the individual futures below into a single future with all of the results
        Future.sequence(AUTHENTICATION_FUNCTION_LIST
                        // Asynchronously invoke all of the functions
                        .map(functionName -> asyncInvokeFunction(functionName, mqttEndpoint)))
                // When we are done find the first defined value, then unwrap the double optional
                .map(results -> results.find(Option::isDefined).flatMap(value -> value))
                .onSuccess(resultHandler::handle);
    }

    private Future<Option<String>> asyncInvokeFunction(String functionName, MqttEndpoint mqttEndpoint) {
        // Asynchronous invocation of the function
        return Future.of(() -> Try.of(() -> syncInvokeFunction(functionName, mqttEndpoint))
                // Log failures
                .onFailure(throwable -> log.error("Lambda invocation failed [" + throwable.getMessage() + "]"))
                // If it fails, return none
                .getOrElse(Option.none()));
    }

    private Option<String> syncInvokeFunction(String functionName, MqttEndpoint mqttEndpoint) {
        InvokeRequest.Builder invokeRequestBuilder = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.REQUEST_RESPONSE);

        HashMap<String, Object> payloadMap = HashMap.of(
                "auth", mqttEndpoint.auth(),
                "client_id", mqttEndpoint.clientIdentifier());

        if (mqttEndpoint.isSsl()) {
            SSLSession sslSession = mqttEndpoint.sslSession();

            Try.of(sslSession::getPeerCertificates)
                    .onSuccess(certificates -> payloadMap.put("peer_certificates", Arrays.asList(certificates)));
        }

        invokeRequestBuilder.payload(SdkBytes.fromUtf8String(new Gson().toJson(payloadMap)));

        InvokeResponse invokeResponse = lazyLambdaClient.get().invoke(invokeRequestBuilder.build());

        if (invokeResponse.statusCode() != 200) {
            throw new RuntimeException("Invoke failed [" + invokeResponse.statusCode() + "]");
        }

        String payload = invokeResponse.payload().asUtf8String();

        return Option.of(payload);
    }

    private void disconnectAndLog(Throwable throwable, MqttEndpoint mqttEndpoint) {
        // All failures must close the endpoint. Don't check mqttEndpoint.isConnected() here!
        //   It will return false because the client isn't fully connected yet.

        // Close the endpoint and ignore any exceptions
        Try.run(mqttEndpoint::close);

        if ((throwable instanceof IllegalStateException) && (throwable.getMessage().contains("MQTT endpoint is closed"))) {
            log.info("Inbound connection disconnected unexpectedly");
            return;
        }

        // Always print the stack trace
        throwable.printStackTrace();

        logExtraInfoIfPossible(throwable);
    }

    private void logExtraInfoIfPossible(Throwable throwable) {
        if (throwable instanceof MqttException) {
            handleMqttExceptions(throwable);
        } else if (throwable instanceof StsException) {
            handleStsExceptions(throwable);
        }
    }

    private void handleStsExceptions(Throwable throwable) {
        if (!throwable.getMessage().contains("Access denied")) {
            return;
        }

        log.error("Access to the role to assume was denied:");
        log.error("- Make sure the role exists");
        log.error(MAKE_SURE_THAT_THE_ROLE_CAN_BE_ASSUMED);
    }

    private void handleMqttExceptions(Throwable throwable) {
        Throwable realException = throwable.getCause();

        if (!realException.getMessage().contains("Incorrect upgrade")) {
            return;
        }

        log.error("There was an issue upgrading this connection to a websocket:");
        log.error("- Make sure the scope down policy is valid");
        log.error(MAKE_SURE_THAT_THE_ROLE_CAN_BE_ASSUMED);
        log.error("- Make sure that Paho is not 1.2.x - https://github.com/aws-amplify/aws-sdk-android/issues/479");
    }

    private void logAndPublish(MqttEndpoint mqttEndpoint, MqttClient mqttClient, MqttPublishMessage message) {
        log.debug("Just received message on [{}] payload [{}] with QoS [{}]",
                message.topicName(),
                message.payload(),
                message.qosLevel());

        disconnectAndLogOnFailure(Try.run(() -> publish(mqttClient, message)), mqttEndpoint);
    }

    private <T> T disconnectAndLogOnFailure(Try<T> inputTry, MqttEndpoint mqttEndpoint) {
        return inputTry
                .onFailure(throwable -> disconnectAndLog(throwable, mqttEndpoint))
                .get();
    }

    private void publish(MqttClient mqttClient, MqttPublishMessage message) throws MqttException {
        mqttClient.publish(message.topicName(), new MqttMessage(message.payload().getBytes()));
    }

    private void close(MqttClient mqttClient) {
        log.debug("Connection closed");

        Try.run(() -> disconnect(mqttClient))
                .recover(MqttException.class, this::ignoreAlreadyDisconnectedClientException)
                .get();
    }

    private Void ignoreAlreadyDisconnectedClientException(MqttException throwable) {
        if (throwable.getReasonCode() == MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED) {
            // Do nothing
            return null;
        }

        throw new RuntimeException(throwable);
    }

    private void disconnect(MqttClient mqttClient) throws MqttException {
        // Disconnect from AWS IoT
        mqttClient.disconnect();
    }

    private void logDisconnect() {
        log.debug("Received disconnect from client");
    }

    private void logPing() {
        // NOTE: We do not pass on pings
        log.debug("Ping received from client");
    }

    private void unsubscribeAndAcknowledge(MqttEndpoint mqttEndpoint, MqttClient mqttClient, MqttUnsubscribeMessage mqttUnsubscribeMessage) {
        // Unsubscribe in AWS IoT to all of the topics requested
        mqttUnsubscribeMessage.topics()
                .forEach(topic -> disconnectAndLogOnFailure(Try.of(() -> unsubscribe(mqttClient, topic)), mqttEndpoint));

        // Acknowledge the unsubscribe request
        disconnectAndLogOnFailure(Try.of(() -> mqttEndpoint.unsubscribeAcknowledge(mqttUnsubscribeMessage.messageId())), mqttEndpoint);
    }

    private void subscribeAndAcknowledge(MqttEndpoint mqttEndpoint, MqttClient mqttClient, MqttSubscribeMessage mqttSubscribeMessage) {
        // Subscribe in AWS IoT to all of the topics requested
        mqttSubscribeMessage.topicSubscriptions()
                .forEach(mqttTopicSubscription -> disconnectAndLogOnFailure(Try.of(() -> subscribe(mqttClient, mqttTopicSubscription)), mqttEndpoint));

        // Acknowledge the subscribe request
        disconnectAndLogOnFailure(Try.of(() -> mqttEndpoint.subscribeAcknowledge(mqttSubscribeMessage.messageId(), grantedQosLevels.asJava())), mqttEndpoint);
    }

    private Void unsubscribe(MqttClient mqttClient, String topic) throws MqttException {
        mqttClient.unsubscribe(topic);
        return null;
    }

    private Void subscribe(MqttClient mqttClient, MqttTopicSubscription mqttTopicSubscription) throws MqttException {
        mqttClient.subscribe(mqttTopicSubscription.topicName());
        return null;
    }

    public static Option<ImmutableRoleToAssume> getRoleToAssume() {
        return Option.of(System.getenv(ROLE_TO_ASSUME))
                .map(string -> ImmutableRoleToAssume.builder().roleToAssume(string).build());
    }
}
