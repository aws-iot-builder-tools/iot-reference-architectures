package com.awssamples.iot.dynamodb.api.handlers;

import com.awssamples.iot.dynamodb.api.SharedHelper;
import com.awssamples.iot.dynamodb.api.handlers.interfaces.HandleIotEvent;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.*;

import static com.awssamples.iot.dynamodb.api.SharedHelper.getGson;

public class HandleIotSendEvent implements HandleIotEvent {
    private static final Logger log = LoggerFactory.getLogger(HandleIotSendEvent.class);

    @Override
    public String getOperationType() {
        return "send";
    }

    @Override
    public String innerHandle(String responseToken, final Map input, String uuid, Optional<String> optionalMessageId, Optional<String> optionalRecipientId) {
        byte[] payload = getPayload(input);
        String recipientUuid = optionalRecipientId.get();

        String sqsMessageId = putInSqs(recipientUuid, payload);

        // Return a payload on the response topic that contains the UUID and confirmation that the message was put in SQS
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put(SharedHelper.UUID_DYNAMO_DB_COLUMN_NAME, uuid);
        payloadMap.put("sqs_message_id", sqsMessageId);

        publishResponse(uuid, optionalMessageId, optionalRecipientId, responseToken, payloadMap);

        return "done";
    }

    private String putInSqs(String uuid, byte[] payload) {
        SqsClient sqsClient = SqsClient.create();

        GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
                .queueName(SharedHelper.getOutboundQueueName())
                .build();

        GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(getQueueUrlRequest);

        // Get the queue URL from the queue name
        String queueUrl = getQueueUrlResponse.queueUrl();

        String encodedPayload = Hex.encodeHexString(payload);

        Map<String, Object> body = new HashMap<>();
        body.put("client_message_id", Math.abs(new Random().nextInt()));
        body.put("message", encodedPayload);
        body.put("imei", uuid);

        String encodedBody = getGson().toJson(body);

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(encodedBody)
                .messageGroupId("DEFAULT")
                .messageDeduplicationId(UUID.randomUUID().toString())
                .build();

        // Send the message to the queue
        SendMessageResponse sendMessageResponse = sqsClient.sendMessage(sendMessageRequest);

        return sendMessageResponse.messageId();
    }

    @Override
    public boolean isMessageIdRequired() {
        // No message ID required
        return false;
    }

    @Override
    public boolean isRecipientUuidRequired() {
        return true;
    }
}
