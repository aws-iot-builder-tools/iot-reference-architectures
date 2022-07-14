package awslabs.client.ssm;

import io.vavr.collection.Stream;
import io.vavr.control.Option;

public enum ClientMessageType {
    INPUT_STREAM_DATA("input_stream_data"),
    OUTPUT_STREAM_DATA("output_stream_data"),
    ACKNOWLEDGE("acknowledge"),
    CHANNEL_CLOSED("channel_closed");

    public static final int LENGTH = 32;
    private final String typeName;
    private Option<byte[]> typeBytes = Option.none();

    ClientMessageType(String typeName) {
        this.typeName = typeName;
    }

    public static Option<ClientMessageType> from(ClientMessage clientMessage) {
        return fromMessageTypeBytes(clientMessage.messageType);
    }

    private static Option<ClientMessageType> fromMessageTypeBytes(byte[] messageType) {
        return fromMessageTypeString(new String(messageType));
    }

    public static Option<ClientMessageType> fromMessageTypeString(String messageType) {
        return Stream.of(values())
                .filter(value -> messageType.contains(value.typeName))
                .toOption();
    }

    public String getTypeName() {
        return typeName;
    }

    public byte[] getTypeBytes() {
        if (typeBytes.isEmpty()) {
            byte[] byteArray = new byte[LENGTH];

            for (int loop = 0; loop < LENGTH - typeName.length(); loop++) {
                byteArray[loop] = 0;
            }

            for (int loop = LENGTH - typeName.length(); loop < LENGTH; loop++) {
                byteArray[loop] = typeName.getBytes()[loop - (LENGTH - typeName.length())];
            }

            typeBytes = Option.of(byteArray);
        }

        return typeBytes.get();
    }
}
