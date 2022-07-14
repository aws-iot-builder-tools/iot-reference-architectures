package awslabs.client.ssm;

import awslabs.client.application.events.SsmData;
import com.google.gwt.event.shared.EventBus;
import elemental2.dom.Blob;
import elemental2.dom.WebSocket;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.Lazy;
import io.vavr.control.Option;

import java.util.concurrent.atomic.AtomicLong;

import static awslabs.client.application.shared.GwtHelper.*;

public class SsmWebSocket {
    private final SsmConfig ssmConfig;
    private final WebSocket webSocket;
    private final EventBus eventBus;
    private final AtomicLong ourSequenceNumber = new AtomicLong(0);
    boolean channelOpen = false;
    private String iotSystemName;
    private Lazy<String> sessionNameLazy = Lazy.of(() -> String.join("-", iotSystemName, uuid().substring(0, 6)));
    private OrderedBuffer<SsmData.Event> orderedBuffer = new OrderedBuffer<>();

    public SsmWebSocket(EventBus eventBus, String iotSystemName, SsmConfig ssmConfig) {
        this.eventBus = eventBus;
        this.iotSystemName = iotSystemName;
        this.ssmConfig = ssmConfig;
        this.webSocket = new WebSocket(ssmConfig.url);
        this.webSocket.onmessage = onMessage();
        this.webSocket.onopen = onOpen();
    }

    public String getSessionName() {
        return sessionNameLazy.get();
    }

    public WebSocket.OnmessageFn onMessage() {
        return event -> {
            // We should receive a blob here
            if (!event.data.isBlob()) {
                info("Not a blob, can't continue");
                return;
            }

            Blob dataBlob = event.data.asBlob();

            dataBlob.arrayBuffer().then(
                    success -> {
                        if (!channelOpen) {
                            // Channel is closed, ignore messages
                            return null;
                        }

                        ClientMessage originalMessage = ClientMessage.from(arrayBufferToByteArray(success));
                        Option<ClientMessageType> clientMessageTypeOption = ClientMessageType.from(originalMessage);

                        if (clientMessageTypeOption.isEmpty()) {
                            // Ignore a message we don't understand (e.g. "pause_publication", "start_publication")
                            return null;
                        }

                        ClientMessageType clientMessageType = clientMessageTypeOption.get();

                        if (clientMessageType.equals(ClientMessageType.ACKNOWLEDGE)) {
                            // Don't send an ACK for an ACK
                            return null;
                        }

                        if (clientMessageType.equals(ClientMessageType.CHANNEL_CLOSED)) {
                            // Don't send an ACK for a channel closed message. Just indicate the channel is closed and close the websocket.
                            channelOpen = false;
                            close();
                            return null;
                        }

                        // Send an ACK for the message
                        ClientMessage ack = ClientMessage.acknowledgementFrom(originalMessage, originalMessage.sequenceNumber);
                        sendBinary(webSocket, ack);

                        if (clientMessageType.equals(ClientMessageType.OUTPUT_STREAM_DATA)) {
                            // Convert the message to an SSM event
                            SsmData.Event ssmDataEvent = new SsmData.Event(this, originalMessage.payload, originalMessage.sequenceNumber);

                            orderedBuffer
                                    // Buffer the current item
                                    .addItem(ssmDataEvent)
                                    // Get whatever items are available
                                    .getNextItems()
                                    // Deliver them
                                    .forEach(eventBus::fireEvent);

                            return null;
                        }

                        MaterialToast.fireToast("Should never reach this code [" + clientMessageType.getTypeName() + "]");
                        return null;
                    },
                    failure -> {
                        MaterialToast.fireToast("Unexpected failure when converting inbound data blob to an array buffer");
                        return null;
                    });
        };
    }

    public WebSocket.OnopenFn onOpen() {
        return event -> {
            channelOpen = true;

            SsmStartMessage ssmStartMessage = new SsmStartMessage(ssmConfig.token);
            String ssmStartMessageJson = ssmStartMessage.toJson();
            webSocket.send(ssmStartMessageJson);

            SsmWindowSizeMessage ssmWindowSizeMessage = new SsmWindowSizeMessage();

            ClientMessage clientMessage = new ClientMessage(ClientMessageType.INPUT_STREAM_DATA, 0, 3, ssmWindowSizeMessage.toJson().getBytes());

            sendBinary(webSocket, clientMessage);
        };
    }

    public void sendKey(int key) {
        byte[] data = new byte[1];
        data[0] = (byte) key;

        if (!channelOpen) {
            MaterialToast.fireToast("This session is closed [" + ssmConfig.token + "]");
            return;
        }

        sendBinary(webSocket, new ClientMessage(ClientMessageType.INPUT_STREAM_DATA, ourSequenceNumber.getAndIncrement(), 1, data));
    }

    public void close() {
        webSocket.close();
    }
}
