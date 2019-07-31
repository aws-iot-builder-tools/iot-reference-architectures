package com.awssamples.iot.dynamodb.api.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.data.CookedMessage;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandleSqsEvent implements RequestHandler<Map, String> {
    private static final String RECORDS = "Records";
    private static final String UUID_KEY = SharedHelper.getEnvironmentVariableOrThrow("uuidKey", HandleSqsEvent::missingUuidKeyException);
    private static final String MESSAGE_ID_KEY = SharedHelper.getEnvironmentVariableOrThrow("messageIdKey", HandleSqsEvent::missingMessageIdKeyException);
    private static final String SQS_QUEUE_ARN = SharedHelper.getEnvironmentVariableOrThrow("sqsQueueArn", HandleSqsEvent::missingSqsQueueArnException);
    private static final String ONLY_ONE_RECORD_MAY_BE_PROCESSED_AT_A_TIME = "Only one record may be processed at a time";

    // Methods that throw exceptions so that the code fails fast when issues come up (values not specified in the environment, etc)

    private static RuntimeException missingUuidKeyException() {
        throw new RuntimeException("Missing the UUID key in the environment, can not continue");
    }

    private static RuntimeException missingMessageIdKeyException() {
        throw new RuntimeException("Missing the message ID key in the environment, can not continue");
    }

    private static RuntimeException missingSqsQueueArnException() {
        throw new RuntimeException("Missing the SQS queue ARN in the environment, can not continue");
    }

    @Override
    public String handleRequest(final Map input, final Context context) {
        List<Map> rawRecords = (List<Map>) input.get(RECORDS);

        if (rawRecords.size() != 1) {
            // Only accept one record at a time
            throw new RuntimeException(ONLY_ONE_RECORD_MAY_BE_PROCESSED_AT_A_TIME);
        }

        // We know we have exactly one record, retrieve it
        Map rawRecord = rawRecords.get(0);

        // Get the SQS specific data from the message
        String receiptHandle = (String) rawRecord.get("receiptHandle");
        String sqsMessageId = (String) rawRecord.get(SharedHelper.MESSAGE_ID);

        // Get the SQS specific attributes from the message and then extract the sent timestamp
        Map attributes = (Map) rawRecord.get("attributes");
        String sentTimestamp = (String) attributes.get("SentTimestamp");

        // Get the map of the values from the body field which is JSON
        Map map = SharedHelper.fromJson((String) rawRecord.get(SharedHelper.BODY), Map.class);

        // Convert the map to a DynamoDB attribute value so it can be stored
        AttributeValue body = SharedHelper.toDynamoDbAttributeValue(map);

        // Create a "cooked message" that has all the fields we want to store
        CookedMessage cookedMessage = new CookedMessage(sentTimestamp, body, sqsMessageId);

        // Store the cooked message in DynamoDB
        addCookedMessageToDynamoDb(cookedMessage);

        // Remove the message from SQS once it is stored in DynamoDB
        removeFromSqs(receiptHandle);

        return "done";
    }

    private void removeFromSqs(String receiptHandle) {
        SqsClient sqsClient = SqsClient.create();

        GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
                .queueName(getQueueName())
                .build();

        GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(getQueueUrlRequest);

        // Get the queue URL from the queue name
        String queueUrl = getQueueUrlResponse.queueUrl();

        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();

        // Delete the message from the queue
        sqsClient.deleteMessage(deleteMessageRequest);
    }

    private String getQueueName() {
        return SQS_QUEUE_ARN.substring(SQS_QUEUE_ARN.lastIndexOf(":") + 1);
    }

    private void addCookedMessageToDynamoDb(CookedMessage cookedMessage) {
        Map<String, AttributeValue> body = cookedMessage.getBody().m();

        // The message ID in DynamoDB is the user specified message ID field, followed by the SQS sent timestamp, followed by the SQS message ID (UUID)
        String messageId = body.get(MESSAGE_ID_KEY).s();
        messageId = String.join("-", messageId, cookedMessage.getSentTimestamp(), cookedMessage.getSqsMessageId());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(SharedHelper.UUID, body.get(UUID_KEY));
        item.put(SharedHelper.MESSAGE_ID, AttributeValue.builder().s(messageId).build());
        item.put(SharedHelper.BODY, cookedMessage.getBody());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .item(item)
                .tableName(SharedHelper.getTableName())
                .build();

        DynamoDbClient.create().putItem(putItemRequest);
    }
}
