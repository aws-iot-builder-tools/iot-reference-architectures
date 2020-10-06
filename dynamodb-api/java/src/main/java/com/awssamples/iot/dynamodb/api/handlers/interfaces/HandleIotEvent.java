package com.awssamples.iot.dynamodb.api.handlers.interfaces;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.google.gson.Gson;
import io.vavr.control.Try;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.awssamples.iot.dynamodb.api.SharedHelper.getGson;

/**
 * Base interface that shares code between the classes that handle IoT messages
 */
public interface HandleIotEvent extends RequestHandler<Map, String> {
    String TOPIC_INPUT_KEY = "topic";
    String HEX_PAYLOAD_KEY = "hex_payload";
    String REQUEST_TOPIC_PREFIX = "RequestTopicPrefix";
    String RESPONSE_TOPIC_TEMPLATE = "ResponseTopicPrefix";

    // Methods that throw exceptions so that the code fails fast when issues come up (values not specified in the environment, etc)
    default RuntimeException missingHexPayloadException() {
        throw new RuntimeException("Hex payload not specified");
    }

    default RuntimeException missingTopicException() {
        throw new RuntimeException("Inbound topic not specified");
    }

    default RuntimeException missingUuidField() {
        throw new RuntimeException("Couldn't find the UUID field in the " + getOperationType() + " topic template");
    }

    default RuntimeException missingRecipientUuidField() {
        throw new RuntimeException("Couldn't find the recipient UUID field in the " + getOperationType() + " topic template");
    }

    default RuntimeException missingMessageIdField() {
        throw new RuntimeException("Couldn't find the message ID field in the " + getOperationType() + " topic template");
    }

    default RuntimeException missingResponseTokenField() {
        throw new RuntimeException("Couldn't find the response token field in the " + getOperationType() + " topic template");
    }

    default RuntimeException missingRequestTopicTemplateException() {
        throw new RuntimeException("Missing the " + getOperationType() + " request topic template in the environment, can not continue");
    }

    default RuntimeException missingResponseTopicTemplateException() {
        throw new RuntimeException("Missing the " + getOperationType() + " response topic template in the environment, can not continue");
    }

    /**
     * @return The type of operation being performed (e.g. get, query, delete, next). This is used to populate exception messages
     * and to determine where to fetch the topic and topic template information in the environment.
     */
    String getOperationType();

    /**
     * @return the MQTT request topic template split into components
     */
    default String[] getSplitRequestTopicTemplate() {
        String requestTopicTemplate = getRequestTopicTemplate();

        requestTopicTemplate = String.join("/", requestTopicTemplate, SharedHelper.UUID_VARIABLE);

        if (isRecipientUuidRequired()) {
            requestTopicTemplate = String.join("/", requestTopicTemplate, SharedHelper.RECIPIENT_UUID_VARIABLE);
        }

        if (isMessageIdRequired()) {
            requestTopicTemplate = String.join("/", requestTopicTemplate, SharedHelper.MESSAGE_ID_VARIABLE);
        }

        requestTopicTemplate = String.join("/", requestTopicTemplate, SharedHelper.TOKEN_VARIABLE);

        return requestTopicTemplate.split("/");
    }

    /**
     * @return the request topic template, for this specific operation type, from the environment or throws an exception
     */
    default String getRequestTopicTemplate() {
        return SharedHelper.getEnvironmentVariableOrThrow(getOperationType() + REQUEST_TOPIC_PREFIX, this::missingRequestTopicTemplateException);
    }

    /**
     * @return the response topic template, for this specific operation type, from the environment or throws an exception
     */
    default String getResponseTopicTemplate() {
        return SharedHelper.getEnvironmentVariableOrThrow(getOperationType() + RESPONSE_TOPIC_TEMPLATE, this::missingResponseTopicTemplateException);
    }

    /**
     * @return the index where we expect to find the UUID in a request topic
     * @throws RuntimeException if the UUID index is not found in the topic template
     */
    default int getUuidRequestTemplateIndex() {
        return findOrThrow(getSplitRequestTopicTemplate(), SharedHelper.UUID_VARIABLE, this::missingUuidField);
    }

    /**
     * @return the index where we expect to find the recipient UUID in a request topic
     * @throws RuntimeException if the recipient UUID index is not found in the topic template
     */
    default int getRecipientUuidRequestTemplateIndex() {
        return findOrThrow(getSplitRequestTopicTemplate(), SharedHelper.RECIPIENT_UUID_VARIABLE, this::missingRecipientUuidField);
    }

    /**
     * @return the index where we expect to find the message ID in a request topic
     * @throws RuntimeException if the message ID index is not found in the topic template
     */
    default int getMessageIdRequestTopicIndex() {
        return findOrThrow(getSplitRequestTopicTemplate(), SharedHelper.MESSAGE_ID_VARIABLE, this::missingMessageIdField);
    }

    /**
     * @return the index where we expect to find the response token in a response topic
     * @throws RuntimeException if the token index is not found in the topic template
     */
    default int getResponseTokenRequestTopicIndex() {
        return findOrThrow(getSplitRequestTopicTemplate(), SharedHelper.TOKEN_VARIABLE, this::missingResponseTokenField);
    }

    /**
     * @param input        an array of input strings to search
     * @param searchString the string being searched for
     * @param thrower      the supplier that will throw an exception if the string is not found
     * @return the index of the search string in the input array
     * @throws RuntimeException if the search string is not found in the input array
     */
    default int findOrThrow(String[] input, String searchString, Supplier<RuntimeException> thrower) {
        return IntStream.range(0, input.length).filter(index -> searchString.equals(input[index])).findFirst().orElseThrow(thrower);
    }

    default byte[] getPayload(Map input) {
        String rawHexPayload = (String) input.get(HEX_PAYLOAD_KEY);
        String hexEncodedPayload = Optional.ofNullable(rawHexPayload).orElseThrow(this::missingHexPayloadException);

        return Try.of(() -> Hex.decodeHex(hexEncodedPayload)).get();
    }

    /**
     * @param input
     * @return the value of the topic key in the input map
     * @throws RuntimeException if the topic key is not found in the input map
     */
    default String getTopic(Map input) {
        return Optional.ofNullable((String) input.get(TOPIC_INPUT_KEY)).orElseThrow(this::missingTopicException);
    }

    /**
     * Shared request handler that invokes the different implementations of this class
     *
     * @param input   the input map from Lambda
     * @param context the context from Lambda
     * @return "done" when complete
     * @throws RuntimeException if any part of the request experiences a failure
     */
    @Override
    default String handleRequest(final Map input, final Context context) {
        // Get the input topic so we can extract the UUID and message ID, if necessary
        String topic = getTopic(input);

        // Split the input topic so we can find the UUID and message ID by index, if necessary
        String[] topicComponents = topic.split("/");

        // Get the UUID
        String uuid = getUuid(topicComponents);

        // Get the message ID, if necessary
        Optional<String> optionalMessageId = getMessageId(topicComponents);

        // Get the recipient UUID, if necessary
        Optional<String> optionalRecipientUuid = getRecipientUuid(topicComponents);

        // Get the token
        String responseToken = getResponseToken(topicComponents);

        // Call the inner handler so the implementations can finish servicing the request
        return innerHandle(responseToken, input, uuid, optionalMessageId, optionalRecipientUuid);
    }

    /**
     * The inner handler that must be implemented for each operation (e.g. get, query, delete, next)
     *
     * @param responseToken         the response token extracted from the inbound message
     * @param input
     * @param uuid                  the device UUID
     * @param optionalMessageId     the message ID, if necessary
     * @param optionalRecipientUuid the recipient UUID, if necessary
     * @return "done" when complete
     */
    String innerHandle(String responseToken, final Map input, String uuid, Optional<String> optionalMessageId, Optional<String> optionalRecipientUuid);

    default String getUuid(String[] topicComponents) {
        // Get the UUID index
        int uuidIndex = getUuidRequestTemplateIndex();

        // Sanity check: Does the topic have enough entries?
        if (topicComponents.length < uuidIndex) {
            // No, throw an exception
            throw new RuntimeException("Topic is too short to provide the UUID at the expected index");
        }

        // Return the UUID wrapped in an optional
        return topicComponents[uuidIndex];
    }

    /**
     * @param topicComponents
     * @return the message ID in this topic, if necessary
     */
    default Optional<String> getMessageId(String[] topicComponents) {
        if (!isMessageIdRequired()) {
            // This implementation does not need the message ID, just return empty
            return Optional.empty();
        }

        // Get the message ID index
        int messageIdIndex = getMessageIdRequestTopicIndex();

        // Sanity check: Does the topic have enough entries?
        if (topicComponents.length < messageIdIndex) {
            // No, throw an exception
            throw new RuntimeException("Topic is too short to provide the message ID at the expected index");
        }

        // Return the message ID wrapped in an optional
        return Optional.of(topicComponents[messageIdIndex]);
    }

    /**
     * @param topicComponents
     * @return the message ID in this topic, if necessary
     */
    default Optional<String> getRecipientUuid(String[] topicComponents) {
        if (!isRecipientUuidRequired()) {
            // This implementation does not need the recipient UUID, just return empty
            return Optional.empty();
        }

        // Get the recipient UUID index
        int recipientUuidIndex = getRecipientUuidRequestTemplateIndex();

        // Sanity check: Does the topic have enough entries?
        if (topicComponents.length < recipientUuidIndex) {
            // No, throw an exception
            throw new RuntimeException("Topic is too short to provide the recipient UUID at the expected index");
        }

        // Return the recipient UUID wrapped in an optional
        return Optional.of(topicComponents[recipientUuidIndex]);
    }

    /**
     * @param topicComponents
     * @return the token this topic, if necessary
     */
    default String getResponseToken(String[] topicComponents) {
        // Get the response token index
        int responseTokenIndex = getResponseTokenRequestTopicIndex();

        // Sanity check: Does the topic have enough entries?
        if (topicComponents.length < responseTokenIndex) {
            // No, throw an exception
            throw new RuntimeException("Topic is too short to provide the response token at the expected index");
        }

        // Return the message ID wrapped in an optional
        return topicComponents[responseTokenIndex];
    }

    /**
     * @return true if a message ID is required for this operation, otherwise false (devices and send do not require a message ID)
     */
    boolean isMessageIdRequired();

    /**
     * @return true if a recipient UUID ID is required for this operation, otherwise false
     */
    boolean isRecipientUuidRequired();

    /**
     * @param responseToken the response token from the caller, used to build the topic
     * @param payloadMap    the payload, specified as a map, to convert to JSON and publish
     */
    default void publishResponse(String uuid, Optional<String> optionalMessageId, Optional<String> optionalRecipientId, String responseToken, Map payloadMap) {
        // Build the topic from this implementation's response topic prefix and the user provided response token
        List<String> dynamicArguments = new ArrayList<>();
        dynamicArguments.add(uuid);
        optionalRecipientId.ifPresent(dynamicArguments::add);
        optionalMessageId.ifPresent(dynamicArguments::add);
        dynamicArguments.add(responseToken);
        String dynamicArgumentString = String.join("/", dynamicArguments);

        String topic = String.join("/", getResponseTopicTemplate(), dynamicArgumentString);

        // Convert the payload map to JSON and then to an SdkBytes object
        SdkBytes payload = SdkBytes.fromString(SharedHelper.toJson(payloadMap), Charset.defaultCharset());

        // Build the publish request
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(topic)
                .payload(payload)
                .build();

        LoggerFactory.getLogger(HandleIotEvent.class).info("LOGGING PUBLISH REQUEST");
        LoggerFactory.getLogger(HandleIotEvent.class).info(getGson().toJson(publishRequest));
        LoggerFactory.getLogger(HandleIotEvent.class).info("LOGGED PUBLISH REQUEST");

        // Publish with the IoT data plane client
        IotDataPlaneClient.create().publish(publishRequest);
    }

    /**
     * @param keyConditions conditions on the row (typically UUID exact match and optionally message ID exact match or relative match)
     * @return the oldest message ID that matches the specified conditions
     */
    default Optional<String> getOldestMessageId(Map<String, Condition> keyConditions) {
        // Scan forward to get the oldest result first
        QueryResponse oldestMessageResponse = getQueryResponse(keyConditions, true);

        return getMessageIdFieldFromDynamoDbRecord(oldestMessageResponse);
    }

    /**
     * @param keyConditions conditions on the row (typically UUID exact match and optionally message ID exact match or relative match)
     * @return the newest message ID that matches the specified conditions
     */
    default Optional<String> getNewestMessageId(Map<String, Condition> keyConditions) {
        // Scan backwards to get the newest result first
        QueryResponse newestMessageResponse = getQueryResponse(keyConditions, false);

        return getMessageIdFieldFromDynamoDbRecord(newestMessageResponse);
    }

    /**
     * @param queryResponse a query response from DynamoDB
     * @return the message ID field from the first returned row, additional rows are ignored
     */
    default Optional<String> getMessageIdFieldFromDynamoDbRecord(QueryResponse queryResponse) {
        return queryResponse.items().stream().findFirst()
                .map(map -> map.get(SharedHelper.MESSAGE_ID_DYNAMO_DB_COLUMN_NAME))
                .map(AttributeValue::s);
    }

    /**
     * @param keyConditions      conditions on the row (typically UUID exact match and optionally message ID exact match or relative match)
     * @param isScanIndexForward true if the results should be returned in ascending order, false if the results should be returned in descending order
     * @return a query response object with 0 or 1 rows that match the key conditions
     */
    default QueryResponse getQueryResponse(Map<String, Condition> keyConditions, boolean isScanIndexForward) {
        QueryRequest queryRequest = QueryRequest.builder()
                .keyConditions(keyConditions)
                .tableName(SharedHelper.getTableName())
                .limit(1)
                .scanIndexForward(isScanIndexForward)
                .build();

        return DynamoDbClient.create().query(queryRequest);
    }
}
