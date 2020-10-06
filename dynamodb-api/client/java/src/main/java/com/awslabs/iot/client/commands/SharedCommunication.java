package com.awslabs.iot.client.commands;

import com.awslabs.iot.client.data.*;

import java.util.List;
import java.util.Optional;

public interface SharedCommunication {
    String UUIDS = "uuids";
    String REQUEST = "request";
    String RESPONSE = "response";
    String DEVICES = "devices";
    String QUERY = "query";
    String GET = "get";
    String NEXT = "next";
    String DELETE = "delete";
    String SEND = "send";

    Optional<List> getUuids();

    Optional<QueryResponse> query();

    Optional<GetResponse> getMessage(String messageId);

    Optional<NextResponse> nextMessage(String messageId);

    Optional<DeleteResponse> deleteMessage(String messageId);

    void getAndDisplayMessage(String messageId);

    Optional<SendResponse> sendMessage(String recipientUuid, String message);
}
