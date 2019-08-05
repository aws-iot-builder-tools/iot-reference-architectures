package com.awssamples.iot.dynamodb.api.handlers.interfaces;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.awssamples.iot.dynamodb.api.SharedHelper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Base interface that shares code between the classes that handle IoT messages
 */
public interface HandleIotEvent extends RequestHandler<Map, String> {
    String TOPIC_INPUT_KEY = "topic";
    String TOKEN_INPUT_KEY = "token";
    String RESPONSE_TOPIC_PREFIX = "ResponseTopicPrefix";
    String TOPIC = "Topic";

    // Methods that throw exceptions so that the code fails fast when issues come up (values not specified in the environment, etc)

    default RuntimeException missingTokenException() {
        throw new RuntimeException("Token for response not specified");
    }

    default RuntimeException missingTopicException() {
        throw new RuntimeException("Inbound topic not specified");
    }

    default RuntimeException missingUuidIndex() {
        throw new RuntimeException("Couldn't find the UUID index in the " + getOperationType() + " topic template");
    }

    default RuntimeException missingMessageIdIndex() {
        throw new RuntimeException("Couldn't find the message ID index in the " + getOperationType() + " topic template");
    }

    default RuntimeException missingResponseTopicPrefixException() {
        throw new RuntimeException("Missing the " + getOperationType() + " response topic prefix in the environment, can not continue");
    }

    default RuntimeException missingTopicTemplateException() {
        throw new RuntimeException("Missing the " + getOperationType() + " topic template in the environment, can not continue");
    }

    /**
     * @return The type of operation being performed (e.g. get, query, delete, next). This is used to populate exception messages
     * and to determine where to fetch the topic and topic template information in the environment.
     */
    String getOperationType();

    /**
     * @return the MQTT topic template split into components
     */
    default String[] getSplitTopicTemplate() {
        return getTopicTemplate().split("/");
    }

    /**
     * @return the response topic prefix, for this specific operation type, from the environment or throws an exception
     */
    default String getResponseTopicPrefix() {
        return SharedHelper.getEnvironmentVariableOrThrow(getOperationType() + RESPONSE_TOPIC_PREFIX, this::missingResponseTopicPrefixException);
    }

    /**
     * @return the topic template, for this specific operation type, from the environment or throws an exception
     */
    default String getTopicTemplate() {
        return SharedHelper.getEnvironmentVariableOrThrow(getOperationType() + TOPIC, this::missingTopicTemplateException);
    }

    /**
     * @return the index where we expect to find the UUID in an inbound topic
     * @throws RuntimeException if the UUID index is not found in the topic template
     */
    default int getUuidIndex() {
        return findOrThrow(getSplitTopicTemplate(), SharedHelper.UUID, this::missingUuidIndex);
    }

    /**
     * @return the index where we expect to find the message ID in an inbound topic
     * @throws RuntimeException if the message ID index is not found in the topic template
     */
    default int getMessageIdIndex() {
        return findOrThrow(getSplitTopicTemplate(), SharedHelper.MESSAGE_ID, this::missingMessageIdIndex);
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

    /**
     * @param input
     * @return the value of the token key in the input map
     * @throws RuntimeException if the token key is not found in the input map
     */
    default String getToken(Map input) {
        return Optional.ofNullable((String) input.get(TOKEN_INPUT_KEY)).orElseThrow(this::missingTokenException);
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
        // Get the response token so we know where to send the reply
        String responseToken = getToken(input);

        // Get the input topic so we can extract the UUID and message ID, if necessary
        String topic = getTopic(input);

        // Split the input topic so we can find the UUID and message ID by index, if necessary
        String[] topicComponents = topic.split("/");

        // Get the UUID, if necessary
        Optional<String> optionalUuid = getUuid(topicComponents);

        // Get the message ID, if necessary
        Optional<String> optionalMessageId = getMessageId(topicComponents);

        // Call the inner handler so the implementations can finish servicing the request
        return innerHandle(responseToken, optionalUuid, optionalMessageId);
    }

    /**
     * The inner handler that must be implemented for each operation (e.g. get, query, delete, next)
     *
     * @param responseToken     the response token extracted from the inbound message
     * @param optionalUuid      the device UUID, if necessary
     * @param optionalMessageId the message ID, if necessary
     * @return "done" when complete
     */
    String innerHandle(String responseToken, Optional<String> optionalUuid, Optional<String> optionalMessageId);

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
        int messageIdIndex = getMessageIdIndex();

        // Sanity check: Does the topic have enough entries?
        if (topicComponents.length < messageIdIndex) {
            // No, throw an exception
            throw new RuntimeException("Topic is too short to provide the message ID at the expected index");
        }

        // Return the message ID wrapped in an optional
        return Optional.of(topicComponents[messageIdIndex]);
    }

    default Optional<String> getUuid(String[] topicComponents) {
        if (!isUuidRequired()) {
            // This implementation does not need the UUID, just return empty
            return Optional.empty();
        }

        // Get the UUID index
        int uuidIndex = getUuidIndex();

        // Sanity check: Does the topic have enough entries?
        if (topicComponents.length < uuidIndex) {
            // No, throw an exception
            throw new RuntimeException("Topic is too short to provide the UUID at the expected index");
        }

        // Return the UUID wrapped in an optional
        return Optional.of(topicComponents[uuidIndex]);
    }

    /**
     * @return true if a message ID is required for this operation, otherwise false
     */
    boolean isMessageIdRequired();

    /**
     * @return true if a UUID is required for this operation, otherwise false
     */
    boolean isUuidRequired();

    /**
     * @param responseToken the response token from the caller, used to build the topic
     * @param payloadMap    the payload, specified as a map, to convert to JSON and publish
     */
    default void publishResponse(String responseToken, Map payloadMap) {
        // Build the topic from this implementation's response topic prefix and the user provided response token
        String topic = String.join("/", getResponseTopicPrefix(), responseToken);

        // Convert the payload map to JSON and then to an SdkBytes object
        SdkBytes payload = SdkBytes.fromString(SharedHelper.toJson(payloadMap), Charset.defaultCharset());

        // Build the publish request
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(topic)
                .payload(payload)
                .build();

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
                .map(map -> map.get(SharedHelper.MESSAGE_ID))
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
