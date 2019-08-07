package io.vertx.fargate;

import com.awssamples.fargate.IotCoreProxyStack;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.awssamples.fargate.IotCoreProxyStack.STACK_NAME_ENVIRONMENT_VARIABLE;

public class SimpleLoadTestIT {
    public static final int PORT = 1883;
    public static final String LOAD_TEST_TOPIC = "load-test";
    public static final String SUBSCRIPTION_TOPIC = String.join("/", LOAD_TEST_TOPIC, "#");
    public static final String CLIENT_COUNT_KEY = "clientCount";
    public static final String MESSAGE_COUNT_KEY = "messageCount";
    public static final String LOCALHOST = "localhost";
    private int clientCount;
    private int messageCount;
    private Vertx vertx;
    private CountDownLatch responseCountDownLatch;
    private CountDownLatch startClientLatch;
    private String nlbEndpoint;
    private int expectedResponseCount;
    private Logger log;

    @Before
    public void setup() {
        log = LoggerFactory.getLogger(SimpleLoadTestIT.class);
        String stackName = Option.of(System.getenv(STACK_NAME_ENVIRONMENT_VARIABLE))
                .orElse(() -> IotCoreProxyStack.stackNameOption)
                .getOrElse(LOCALHOST);

        String clientCountString = Option.of(System.getenv(CLIENT_COUNT_KEY)).getOrElse("10");
        clientCount = Integer.parseInt(clientCountString);

        String messageCountString = Option.of(System.getenv(MESSAGE_COUNT_KEY)).getOrElse("10");
        messageCount = Integer.parseInt(messageCountString);

        if (stackName.equals(LOCALHOST)) {
            // Not the stack name, just localhost testing
            nlbEndpoint = LOCALHOST;
        } else if (stackName.contains(".")) {
            // CloudFormation stack names cannot contain periods, treat this as a host name for the NLB
            nlbEndpoint = stackName;
        } else {
            Stack stack = Helper.getCloudFormationStack();

            // Get the NLB endpoint from CloudFormation
            Option<Output> nlbEndpointOption = Stream.ofAll(stack.outputs())
                    .filter(output -> output.outputKey().equals("NLBEndpoint"))
                    .headOption();

            if (nlbEndpointOption.isEmpty()) {
                Assert.fail("No NLB endpoint found for stack [" + stackName + "]");
            }

            nlbEndpoint = nlbEndpointOption.get().outputValue();
        }

        expectedResponseCount = clientCount * messageCount;
        responseCountDownLatch = new CountDownLatch(expectedResponseCount);
        startClientLatch = new CountDownLatch(clientCount);

        vertx = Vertx.vertx();
    }

    @Test
    public void shouldHandleXClientsYMessagesEach() throws InterruptedException {
        shouldHandleXClientsYMessagesEach(clientCount, messageCount);
    }

    private void shouldHandleXClientsYMessagesEach(int clientCount, int messageCount) throws InterruptedException {
        String expectedTopic = String.join("/", LOAD_TEST_TOPIC, UUID.randomUUID().toString());
        String expectedPayload = UUID.randomUUID().toString();

        IntStream.range(0, clientCount)
                .peek(clientNumber -> log.info("Client #" + clientNumber + " connecting"))
                .mapToObj(v -> createClient())
                .map(client -> client.publishHandler(mqttPublishMessage -> createPublishHandler(expectedTopic, expectedPayload, mqttPublishMessage)))
                .forEach(this::connectAndSubscribeToAll);

        log.info("Waiting for all clients to connect...");

        Instant startClientReady = Instant.now();

        if (!startClientLatch.await(2, TimeUnit.MINUTES)) {
            Assert.fail("Not all clients connected within the timeout period.  Expected [" + clientCount + "], actual [" + (clientCount - startClientLatch.getCount()) + "]");
        }

        Instant endClientReady = Instant.now();

        log.info("All clients connected and subscribed to all topics");

        // Connect a publish a message to the clients
        MqttClient publisher = createClient();

        publisher.connect(PORT, nlbEndpoint, mqttConnAckMessageAsyncResult -> {
            // Publish all messages at QoS0
            IntStream.range(0, messageCount)
                    .peek(count -> log.debug("Publish " + count))
                    .forEach(v -> publisher.publish(expectedTopic, Buffer.buffer(expectedPayload), MqttQoS.AT_MOST_ONCE, false, false));
        });

        log.info("Waiting to receive all " + expectedResponseCount + " messages...");

        Instant startResponses = Instant.now();

        if (!responseCountDownLatch.await(2, TimeUnit.MINUTES)) {
            Assert.fail("Not all clients received the message from the broker within the timeout period.  Expected [" + expectedResponseCount + "], actual [" + (expectedResponseCount - responseCountDownLatch.getCount()) + "]");
        }

        Instant endResponses = Instant.now();

        Duration clientConnectionDuration = Duration.between(startClientReady, endClientReady);
        log.info("Approximate time for all clients to connect - " + clientConnectionDuration.toMillis() + "ms");
        log.info("Average time per client connection - " + ((double) clientConnectionDuration.toMillis() / (double) clientCount) + "ms");
        Duration totalRoundTripTime = Duration.between(startResponses, endResponses);
        log.info("Approximate round-trip time for all messages - " + totalRoundTripTime.toMillis() + "ms");
        log.info("Average round-trip time per message - " + ((double) totalRoundTripTime.toMillis() / (double) expectedResponseCount) + "ms");
    }

    private void createPublishHandler(String expectedTopic, String expectedPayload, MqttPublishMessage mqttPublishMessage) {
        String receivedTopic = mqttPublishMessage.topicName();
        String receivedPayload = new String(mqttPublishMessage.payload().getBytes());

        log.debug("Received [" + receivedPayload + "] on topic [" + receivedTopic + "]");

        if (receivedTopic.equals(expectedTopic) && receivedPayload.equals(expectedPayload)) {
            responseCountDownLatch.countDown();
        } else {
            Assert.fail("Incorrect payload received");
        }
    }

    private void connectAndSubscribeToAll(MqttClient client) {
        client.connect(PORT, nlbEndpoint, mqttConnAckMessageAsyncResult -> {
            if (mqttConnAckMessageAsyncResult.failed()) {
                Assert.fail("Connection to broker failed, is the broker running?");
            }

            // Subscribe to all topics at QoS0
            client.subscribe(SUBSCRIPTION_TOPIC, 0, subscribeSentHandler -> {
                if (subscribeSentHandler.succeeded()) {
                    startClientLatch.countDown();
                }
            });
        });
    }

    private MqttClient createClient() {
        MqttClientOptions options = new MqttClientOptions();
        options.setUsername("use-token-auth");
        options.setPassword("INTEGRATION-TEST-" + UUID.randomUUID());
        options.setClientId("INTEGRATION-TEST-" + UUID.randomUUID());
        // Default keep alive interval is 30 seconds. We increase that to 999 to avoid sending pings during our test.
        // If this is left to the default, and the systems get bogged down, we will see exceptions like this:
        //   java.lang.IllegalStateException: Received an MQTT packet from a not connected client (CONNECT not sent yet)
        options.setKeepAliveInterval(999);

        return MqttClient.create(vertx, options);
    }
}
