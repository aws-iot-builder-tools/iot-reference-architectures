package awslabs.client.ssm;

import com.google.gwt.user.client.Window;
import io.vavr.Tuple;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Random;
import java.util.stream.IntStream;

import static awslabs.client.application.shared.GwtHelper.*;

public class ClientMessage {
    private static Random random = new Random();
    public Integer headerLength;  // 4 bytes
    public byte[] messageType;    // 32 bytes
    public Integer schemaVersion; // 4 bytes
    public Long createdDate;      // 8 bytes
    public Long sequenceNumber;   // 8 bytes
    public byte[] flags;          // 8 bytes, 128 bits of flags
    public byte[] messageId;      // 16 bytes
    public byte[] payloadDigest;  // 32 bytes
    public Integer payloadType;   // 4 bytes
    public Integer payloadLength; // 4 bytes
    public byte[] payload;        // Variable length
    public String payloadString;
    private String current;
    private String remaining;

    public ClientMessage(ClientMessageType clientMessageType, long sequenceNumber, int payloadType, byte[] payload) {
        this.messageType = clientMessageType.getTypeBytes();
        this.sequenceNumber = sequenceNumber;
        this.payloadType = payloadType;
        this.payload = payload;
    }

    public ClientMessage(String original) {
        this.remaining = original;
    }

    public static ClientMessage from(byte[] byteArrayMessage) {
        return from(bytesToHexString(byteArrayMessage, byteArrayMessage.length));
    }

    public static ClientMessage acknowledgementFrom(ClientMessage originalMessage, long sequenceNumber) {
        AcknowledgeContent acknowledgeContent = new AcknowledgeContent(originalMessage);
        // Payload type appears to always be zero
        return new ClientMessage(ClientMessageType.ACKNOWLEDGE, sequenceNumber, 0, acknowledgeContent.toJson().getBytes());
    }

    public static ClientMessage from(String binaryStringMessage) {
        // If the data doesn't fall on a byte boundary this approach won't work!
        if ((binaryStringMessage.length() % 2) != 0) {
            Window.alert("length: " + binaryStringMessage.length());
            throw new RuntimeException("The length of the binary data must be evenly divisible by 2 or this code will produce unexpected results");
        }

        binaryStringMessage = hexStringToBinary(binaryStringMessage);

        ClientMessage clientMessage = new ClientMessage(binaryStringMessage);
        getBytesFromString(clientMessage, 4);
        clientMessage.headerLength = getInt(clientMessage);
        getBytesFromString(clientMessage, 32);
        clientMessage.messageType = getBytes(clientMessage);
        getBytesFromString(clientMessage, 4);
        clientMessage.schemaVersion = getInt(clientMessage);
        getBytesFromString(clientMessage, 8);
        clientMessage.createdDate = getLong(clientMessage);
        getBytesFromString(clientMessage, 8);
        clientMessage.sequenceNumber = getLong(clientMessage);
        getBytesFromString(clientMessage, 8);
        clientMessage.flags = getBytes(clientMessage);
        getBytesFromString(clientMessage, 16);
        clientMessage.messageId = getBytes(clientMessage);
        getBytesFromString(clientMessage, 32);
        clientMessage.payloadDigest = getBytes(clientMessage);
        getBytesFromString(clientMessage, 4);
        clientMessage.payloadType = getInt(clientMessage);
        getBytesFromString(clientMessage, 4);
        clientMessage.payloadLength = getInt(clientMessage);
        getBytesFromString(clientMessage, clientMessage.payloadLength);
        clientMessage.payload = getBytes(clientMessage);
        clientMessage.payloadString = new String(clientMessage.payload);
        clientMessage.current = "";

        return clientMessage;
    }

    private static byte[] getBytes(ClientMessage clientMessage) {
        return bitsToBytes(clientMessage.current);
    }

    private static String getString(ClientMessage clientMessage) {
        return bitsToString(clientMessage.current);
    }

    private static int getInt(ClientMessage clientMessage) {
        return (int) getInt(clientMessage.current);
    }

    private static long getInt(String bitString) {
        return new BigInteger(bitString, 2).longValueExact();
    }

    private static long getLong(ClientMessage clientMessage) {
        return getLong(clientMessage.current);
    }

    private static long getLong(String bitString) {
        return new BigInteger(bitString, 2).longValue();
    }

    @NotNull
    private static String bitsToString(String bitString) {
        return new String(bitsToBytes(bitString));
    }

    private static byte[] bitsToBytes(String bitString) {
        int chunkSize = 8;
        int numberOfChunks = (bitString.length() + chunkSize - 1) / chunkSize;

        byte[] byteArray = new byte[numberOfChunks];

        Stream.ofAll(IntStream.range(0, numberOfChunks)
                        .mapToObj(index -> bitString.substring(index * chunkSize, Math.min((index + 1) * chunkSize, bitString.length())))
                        .map(binary -> Integer.parseInt(binary, 2))
                        .map(Integer::byteValue))
                .zipWithIndex()
                .forEach(tuple -> byteArray[tuple._2] = tuple._1);

        return byteArray;
    }

    private static void getBytesFromString(ClientMessage clientMessage, int byteLength) {
        getBitsFromString(clientMessage, byteLength * 8);
    }

    private static void getBitsFromString(ClientMessage clientMessage, int bitLength) {
        Tuple.of(clientMessage.remaining, clientMessage.remaining)
                .map1(value -> value.substring(0, bitLength))
                .map1(clientMessage::current)
                .map((value1, value2) -> Tuple.of(value1, value2.substring(value1.length())))
                .map2(clientMessage::remaining);
    }

    public String current(String current) {
        this.current = current;

        return this.current;
    }

    public String remaining(String remaining) {
        this.remaining = remaining;

        return this.remaining;
    }

    public String toHexString() {
        populateMissingValues();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(bytesToHexString(headerLength, 4));
        stringBuilder.append(bytesToHexString(messageType, 32));
        stringBuilder.append(bytesToHexString(schemaVersion, 4));
        stringBuilder.append(bytesToHexString(createdDate, 8));
        stringBuilder.append(bytesToHexString(sequenceNumber, 8));
        stringBuilder.append(bytesToHexString(flags, 8));
        stringBuilder.append(bytesToHexString(messageId, 16));
        stringBuilder.append(bytesToHexString(payloadDigest, 32));
        stringBuilder.append(bytesToHexString(payloadType, 4));
        stringBuilder.append(bytesToHexString(payloadLength, 4));
        stringBuilder.append(bytesToHexString(payload, payload.length));

        return stringBuilder.toString();
    }

    private void populateMissingValues() {
        if (messageType == null) {
            throw new RuntimeException("The message type must be specified");
        }

        if (payload == null) {
            throw new RuntimeException("The payload must be specified");
        }

        if (payloadType == null) {
            throw new RuntimeException("The payload type must be specified");
        }

        if (sequenceNumber == null) {
            throw new RuntimeException("The sequence number must be specified");
        }

        if (headerLength == null) {
            headerLength = 116;
        }

        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        if (createdDate == null) {
            createdDate = System.currentTimeMillis();
        }

        if (flags == null) {
            flags = new byte[0];
        }

        if (messageId == null) {
            messageId = new byte[16];
            random.nextBytes(messageId);
        }

        if (payloadDigest == null) {
            payloadDigest = Try.of(() -> MessageDigest.getInstance("SHA-256"))
                    .map(messageDigest -> messageDigest.digest(payload))
                    .get();
        }

        if (payloadLength == null) {
            payloadLength = payload.length;
        }
    }

    public byte[] toByteArray() {
        return hexStringToBytes(toHexString());
    }
}
