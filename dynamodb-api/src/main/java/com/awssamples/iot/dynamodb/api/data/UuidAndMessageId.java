package com.awssamples.iot.dynamodb.api.data;

public class UuidAndMessageId {
    private final String uuid;
    private final String messageId;

    public UuidAndMessageId(String uuid, String messageId) {
        this.uuid = uuid;
        this.messageId = messageId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getMessageId() {
        return messageId;
    }
}
