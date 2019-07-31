package com.awssamples.iot.dynamodb.api;

import com.google.gson.Gson;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.utils.CollectionUtils.toMap;

public class SharedHelper {
    public static final String ERROR_KEY = "error";
    public static final String UUID = "uuid";
    public static final String MESSAGE_ID = "messageId";
    public static final String BODY = "body";
    private static final String DYNAMODB_TABLE_ARN = getEnvironmentVariableOrThrow("dynamoDbTableArn", SharedHelper::dynamoDbTableArnMissingException);

    private static RuntimeException dynamoDbTableArnMissingException() {
        throw new RuntimeException("Missing the DynamoDB table ARN in the environment, can not continue");
    }

    /**
     * @return the DynamoDB table name parsed from the DynamoDB table ARN specified in the environment variable
     */
    public static String getTableName() {
        return DYNAMODB_TABLE_ARN.substring(DYNAMODB_TABLE_ARN.lastIndexOf("/") + 1);
    }

    /**
     * @param object a normal Java object
     * @return a DynamoDB attribute value that represents the input object
     */
    public static AttributeValue toDynamoDbAttributeValue(Object object) {
        // NOTE: This code was borrowed from Amazon Web Services DynamoDB Mapper v1
        if (object instanceof AttributeValue) {
            return (AttributeValue) object;
        }
        if (object instanceof String) {
            return AttributeValue.builder().s((String) object).build();
        }
        if (object instanceof Number) {
            return AttributeValue.builder().n(String.valueOf((Number) object)).build();
        }
        if (object instanceof byte[]) {
            return AttributeValue.builder().b(SdkBytes.fromByteArray((byte[]) object)).build();
        }
        if (object instanceof ByteBuffer) {
            return AttributeValue.builder().b(SdkBytes.fromByteBuffer((ByteBuffer) object)).build();
        }
        if (object instanceof Boolean) {
            return AttributeValue.builder().bool((Boolean) object).build();
        }
        if (object instanceof List) {
            List<AttributeValue> attributeValues = ((List<?>) object).stream()
                    .map(SharedHelper::toDynamoDbAttributeValue)
                    .collect(toList());
            return AttributeValue.builder().l(attributeValues).build();
        }
        if (object instanceof Map) {
            Map<String, AttributeValue> attributeValues =
                    ((Map<String, ?>) object).entrySet()
                            .stream()
                            .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), toDynamoDbAttributeValue(e.getValue())))
                            .collect(toMap());
            return AttributeValue.builder().m(attributeValues).build();
        }
        throw new IllegalArgumentException("Unsupported type: " + object.getClass());
    }

    /**
     * @param attributeValue a DynamoDB attribute value
     * @return a normal Java object that represents input attribute value
     */
    private static Object fromDynamoDbAttributeValue(AttributeValue attributeValue) {
        if (attributeValue.s() != null) {
            return attributeValue.s();
        }

        if (attributeValue.n() != null) {
            return attributeValue.n();
        }

        if (attributeValue.b() != null) {
            return attributeValue.b();
        }

        if (!attributeValue.bs().isEmpty()) {
            return attributeValue.bs();
        }

        if (!attributeValue.ss().isEmpty()) {
            return attributeValue.ss();
        }

        if (!attributeValue.ns().isEmpty()) {
            return attributeValue.ns();
        }

        if (attributeValue.bool() != null) {
            return attributeValue.bool();
        }

        if (!attributeValue.l().isEmpty()) {
            return attributeValue.l();
        }

        if (attributeValue.m() != null) {
            return attributeValue.m().entrySet()
                    .stream()
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), fromDynamoDbAttributeValue(e.getValue())))
                    .collect(toMap());
        }

        throw new IllegalArgumentException("Unsupported attribute value: " + attributeValue);
    }

    /**
     * A helper function to convert a map entry with an attribute value to a normal Java key value pair
     *
     * @param entry
     * @return
     */
    public static AbstractMap.SimpleEntry<String, Object> fromDynamoDbAttributeValue(Map.Entry<String, AttributeValue> entry) {
        return fromDynamoDbAttributeValue(entry.getKey(), entry.getValue());
    }

    /**
     * A helper function to convert a name and an attribute value to a normal Java key value pair
     *
     * @param name
     * @param attributeValue
     * @return
     */
    private static AbstractMap.SimpleEntry<String, Object> fromDynamoDbAttributeValue(String name, AttributeValue attributeValue) {
        Object object = fromDynamoDbAttributeValue(attributeValue);

        return new AbstractMap.SimpleEntry<>(name, object);
    }

    /**
     * @param input an object to convert to JSON
     * @return a JSON string representing the input object
     */
    public static String toJson(Object input) {
        return new Gson().toJson(input);
    }

    /**
     * @param input a JSON string to convert to an object
     * @param type  the type of the output object
     * @return an object, with the specified class, created from the JSON string
     */
    public static <T> T fromJson(String input, Class<T> type) {
        return new Gson().fromJson(input, type);
    }

    /**
     * @param name                     the name of a variable to retrieve from the environment
     * @param runtimeExceptionSupplier the supplier that will throw an exception if the variable is not found
     * @return the value of the variable in the environment
     */
    public static String getEnvironmentVariableOrThrow(String name, Supplier<RuntimeException> runtimeExceptionSupplier) {
        return Optional.ofNullable(System.getenv(name)).orElseThrow(runtimeExceptionSupplier);
    }
}
