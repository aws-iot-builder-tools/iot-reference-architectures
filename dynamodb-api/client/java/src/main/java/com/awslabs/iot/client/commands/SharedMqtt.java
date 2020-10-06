package com.awslabs.iot.client.commands;

import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.applications.Arguments;
import com.awslabs.iot.client.data.*;
import com.awslabs.iot.client.helpers.iot.interfaces.WebsocketsHelper;
import io.vavr.control.Try;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

public class SharedMqtt implements SharedCommunication {
    private final Logger log = LoggerFactory.getLogger(SharedMqtt.class);
    @Inject
    WebsocketsHelper websocketsHelper;
    @Inject
    Arguments arguments;
    @Inject
    JsonHelper jsonHelper;
    private static final long MAX_SLEEP_TIME_MS = 10000;
    private static final int SLEEP_TIME_MS = 100;
    private static final long MAX_SLEEPS = MAX_SLEEP_TIME_MS / SLEEP_TIME_MS;
    private Optional<MqttClient> optionalMqttClient = Optional.empty();

    @Inject
    public SharedMqtt() {
    }

    private <T> Optional<T> getResponseFromMqtt(Class<T> inputOutputClass, String operation, Optional<String> optionalRecipientUuid, Optional<String> optionalMessageId, Optional<String> optionalPayload) {
        return getResponseFromMqtt(inputOutputClass, inputOutputClass, operation, input -> input, optionalRecipientUuid, optionalMessageId, optionalPayload);
    }

    private <T, V> Optional<V> getResponseFromMqtt(Class<T> inputClass, Class<V> outputClass, String operation, Function<T, V> messageHandler, Optional<String> optionalRecipientUuid, Optional<String> optionalMessageId, Optional<String> optionalPayload) {
        if (!optionalMqttClient.isPresent()) {
            optionalMqttClient = Optional.of(Try.of(() -> websocketsHelper.connectMqttClient()).get());
        }

        MqttClient mqttClient = optionalMqttClient.get();

        String token = UUID.randomUUID().toString();

        List<String> dynamicArgumentList = new ArrayList<>();
        dynamicArgumentList.add(arguments.uuid);
        optionalRecipientUuid.ifPresent(dynamicArgumentList::add);
        optionalMessageId.ifPresent(dynamicArgumentList::add);

        String dynamicArguments = String.join("/", dynamicArgumentList);

        String responseTopic = String.join("/", RESPONSE, operation, dynamicArguments, token);

        @SuppressWarnings("unchecked") final V[] returnValue = (V[]) Array.newInstance(outputClass, 1);
        returnValue[0] = null;

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (!topic.equals(responseTopic)) {
                    log.warn("Received an unexpected message on topic [" + topic + "]");
                    return;
                }

                T payload = jsonHelper.fromJson(inputClass, message.getPayload());
                returnValue[0] = messageHandler.apply(payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        Try.run(() -> mqttClient.subscribe(responseTopic, 1)).get();
        String requestTopic = String.join("/", REQUEST, operation, dynamicArguments, token);

        String payload = optionalPayload.orElse("{}");
        Try.run(() -> mqttClient.publish(requestTopic, payload.getBytes(), 0, false)).get();

        // Wait
        long sleepCounter = 0;

        while ((returnValue[0] == null) && (sleepCounter++ < MAX_SLEEPS)) {
            Try.run(() -> Thread.sleep(SLEEP_TIME_MS)).get();
        }

        if (returnValue[0] == null) {
            log.warn("No response received, has the stack been launched?");
        }

        // Unsubscribe from the topic so we don't hit the subscription limit on this connection
        Try.run(() -> mqttClient.unsubscribe(responseTopic)).get();

        return Optional.ofNullable(returnValue[0]);
    }

    private void safeCloseAndDisconnect(MqttClient mqttClient) {
        Try.run(mqttClient::disconnect)
                .onFailure(throwable -> log.info(String.join("", "Exception: [", throwable.getMessage(), "]")));
        Try.run(() -> websocketsHelper.close(mqttClient))
                .onFailure(throwable -> log.info(String.join("", "Exception: [", throwable.getMessage(), "]")));
    }

    @Override
    public Optional<List> getUuids() {
        return getResponseFromMqtt(Map.class, List.class, DEVICES, this::getUuidsOrLogError,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private List<String> getUuidsOrLogError(Map map) {
        if (!map.containsKey(UUIDS)) {
            log.error("No UUIDs in payload [" + map.toString() + "]");
            return null;
        }

        return (List<String>) map.get(UUIDS);
    }

    @Override
    public Optional<QueryResponse> query() {
        return getResponseFromMqtt(QueryResponse.class, QUERY, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Override
    public Optional<GetResponse> getMessage(String messageId) {
        return getResponseFromMqtt(GetResponse.class, GET, Optional.empty(), Optional.of(messageId), Optional.empty());
    }

    @Override
    public Optional<NextResponse> nextMessage(String messageId) {
        return getResponseFromMqtt(NextResponse.class, NEXT, Optional.empty(), Optional.of(messageId), Optional.empty());
    }

    @Override
    public Optional<DeleteResponse> deleteMessage(String messageId) {
        return getResponseFromMqtt(DeleteResponse.class, DELETE, Optional.empty(), Optional.of(messageId), Optional.empty());
    }

    @Override
    public void getAndDisplayMessage(String messageId) {
        Optional<GetResponse> optionalGetResponse = getMessage(messageId);

        if (!optionalGetResponse.isPresent()) {
            log.error("No get response received");
            return;
        }

        GetResponse getResponse = optionalGetResponse.get();

        if (getResponse.getPayload().isPresent()) {
            log.info("[" + messageId + "] - " + getResponse.getPayload().get());
        } else {
            log.warn("[" + messageId + "] No payload");
        }
    }

    @Override
    public Optional<SendResponse> sendMessage(String recipientUuid, String message) {
        return getResponseFromMqtt(Map.class, SendResponse.class, SEND, SendResponse::fromMap, Optional.of(recipientUuid), Optional.empty(), Optional.of(message));
    }
}
