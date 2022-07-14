package awslabs.client.ssm;

import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.*;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple4;
import io.vavr.control.Try;

import static awslabs.client.application.shared.GwtHelper.info;
import static awslabs.client.application.shared.GwtHelper.singleByteToHexString;

public class AcknowledgeContent {
    // This is lazy so that this class can be used in tests without throwing a GWT.create error
    private static Lazy<AcknowledgeContentMapper> mapperLazy = Lazy.of(() -> GWT.create(AcknowledgeContentMapper.class));
    public String AcknowledgedMessageType;
    public String AcknowledgedMessageId;
    public Long AcknowledgedMessageSequenceNumber;
    public boolean IsSequentialMessage;
    public AcknowledgeContent() {
    }

    public AcknowledgeContent(String AcknowledgedMessageType,
                              String AcknowledgedMessageId,
                              Long AcknowledgedMessageSequenceNumber,
                              boolean IsSequentialMessage) {
        this.AcknowledgedMessageType = AcknowledgedMessageType;
        this.AcknowledgedMessageId = AcknowledgedMessageId;
        this.AcknowledgedMessageSequenceNumber = AcknowledgedMessageSequenceNumber;
        this.IsSequentialMessage = IsSequentialMessage;
    }

    public AcknowledgeContent(ClientMessage clientMessage) {
        this.AcknowledgedMessageType = ClientMessageType.from(clientMessage)
                .map(ClientMessageType::getTypeName)
                .getOrElseThrow(() -> new RuntimeException("Could not determine the message type"));
        this.AcknowledgedMessageId = messageIdToUuidString(clientMessage.messageId);
        this.AcknowledgedMessageSequenceNumber = clientMessage.sequenceNumber;
        this.IsSequentialMessage = true;
    }

    public AcknowledgeContent(String AcknowledgedMessageType,
                              byte[] messageId,
                              Long AcknowledgedMessageSequenceNumber,
                              boolean IsSequentialMessage) {
        this.AcknowledgedMessageType = AcknowledgedMessageType;
        this.AcknowledgedMessageId = messageIdToUuidString(messageId);
        this.AcknowledgedMessageSequenceNumber = AcknowledgedMessageSequenceNumber;
        this.IsSequentialMessage = IsSequentialMessage;
    }

    public AcknowledgeContent(Tuple4<String, String, Long, Boolean> tuple) {
        this.AcknowledgedMessageType = tuple._1;
        this.AcknowledgedMessageId = tuple._2;
        this.AcknowledgedMessageSequenceNumber = tuple._3;
        this.IsSequentialMessage = tuple._4;
    }

    public static AcknowledgeContent fromJson(String input) {
        Tuple4<String, String, Long, Boolean> tuple = Try.of(() -> JSONParser.parseStrict(input))
                .map(JSONValue::isObject)
                .map(value -> Tuple.of(
                        value.get("AcknowledgedMessageType"),
                        value.get("AcknowledgedMessageId"),
                        value.get("AcknowledgedMessageSequenceNumber"),
                        value.get("IsSequentialMessage")))
                .onFailure(throwable -> info("Exception -- " + throwable.getMessage()))
                .onSuccess(value -> info("Success?"))
                .get()
                .map1(JSONValue::isString)
                .map1(JSONString::stringValue)
                .map2(JSONValue::isString)
                .map2(JSONString::stringValue)
                .map3(JSONValue::isNumber)
                .map3(JSONNumber::doubleValue)
                .map3(Double::longValue)
                .map4(JSONValue::isBoolean)
                .map4(JSONBoolean::booleanValue);

        return new AcknowledgeContent(tuple);
    }

    public static String messageIdToUuidString(byte[] messageId) {
        // Given a UUID:
        // value     - ac64a630-cf2b-e4f9-d4e8-6282ef504949
        // positions - 00112233-4455-6677-8899-aabbccddeeff
        //
        // It must be translated to this:
        // value     - d4e86282-ef50-4949-ac64-a630cf2be4f9
        //
        // The output positions are then translated like this:
        // 8899aabb-ccdd-eeff-0011-223344556677

        byte[] returnValue = new byte[16];

        returnValue[0] = messageId[8];
        returnValue[1] = messageId[9];
        returnValue[2] = messageId[10];
        returnValue[3] = messageId[11];
        returnValue[4] = messageId[12];
        returnValue[5] = messageId[13];
        returnValue[6] = messageId[14];
        returnValue[7] = messageId[15];

        returnValue[8] = messageId[0];
        returnValue[9] = messageId[1];
        returnValue[10] = messageId[2];
        returnValue[11] = messageId[3];
        returnValue[12] = messageId[4];
        returnValue[13] = messageId[5];
        returnValue[14] = messageId[6];
        returnValue[15] = messageId[7];

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(singleByteToHexString(returnValue[0]));
        stringBuilder.append(singleByteToHexString(returnValue[1]));
        stringBuilder.append(singleByteToHexString(returnValue[2]));
        stringBuilder.append(singleByteToHexString(returnValue[3]));
        stringBuilder.append("-");
        stringBuilder.append(singleByteToHexString(returnValue[4]));
        stringBuilder.append(singleByteToHexString(returnValue[5]));
        stringBuilder.append("-");
        stringBuilder.append(singleByteToHexString(returnValue[6]));
        stringBuilder.append(singleByteToHexString(returnValue[7]));
        stringBuilder.append("-");
        stringBuilder.append(singleByteToHexString(returnValue[8]));
        stringBuilder.append(singleByteToHexString(returnValue[9]));
        stringBuilder.append("-");
        stringBuilder.append(singleByteToHexString(returnValue[10]));
        stringBuilder.append(singleByteToHexString(returnValue[11]));
        stringBuilder.append(singleByteToHexString(returnValue[12]));
        stringBuilder.append(singleByteToHexString(returnValue[13]));
        stringBuilder.append(singleByteToHexString(returnValue[14]));
        stringBuilder.append(singleByteToHexString(returnValue[15]));

        return stringBuilder.toString();
    }

    public String toJson() {
        return mapperLazy.get().write(this);
    }

    public interface AcknowledgeContentMapper extends ObjectMapper<AcknowledgeContent> {
    }
}
