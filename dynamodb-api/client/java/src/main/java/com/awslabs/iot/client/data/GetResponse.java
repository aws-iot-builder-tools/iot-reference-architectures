package com.awslabs.iot.client.data;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import io.vavr.control.Try;
import org.apache.commons.codec.binary.Hex;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

@Gson.TypeAdapters
@Value.Immutable
public abstract class GetResponse {
    public static final String PAYLOAD = "payload";
    public static final String DATA = "data";

    public abstract String getMessageId();

    public abstract Map<String, Object> getBody();

    public Optional<String> getPayload() {
        Map<String, Object> body = getBody();

        if (!body.containsKey(DATA)) {
            return Optional.empty();
        }

        Map<String, Object> data = (Map<String, Object>) body.get(DATA);

        if (!data.containsKey(PAYLOAD)) {
            return Optional.empty();
        }

        String encodedPayloadString = (String) data.get(PAYLOAD);

        String prefix = "";
        String suffix = "";

        // Preprocess messages
        if (encodedPayloadString.startsWith("0100") && (encodedPayloadString.endsWith("00"))) {
            // Looks like a text message, trim the leading four characters (two bytes) and the NULL terminator
            encodedPayloadString = encodedPayloadString.substring(4, encodedPayloadString.length() - 2);
            prefix = "Text message payload [";
            suffix = "]";
        }

        String finalEncodedPayloadString = encodedPayloadString;
        byte[] binaryPayload = Try.of(() -> Hex.decodeHex(finalEncodedPayloadString.toCharArray())).get();

        String decodedPayload = null;

        // Postprocess messages
        if (encodedPayloadString.startsWith("04")) {
            // Post-process GPS payloads into a human readable format
            GseOpenGps10Byte gseOpenGps10Byte = parseGseOpenGps10BytePayload(binaryPayload);
            decodedPayload = gseOpenGps10Byte.getLatitude() + ", " + gseOpenGps10Byte.getLongitude();
            prefix = "10-byte GSE Open GPS payload [";
            suffix = "]";
        } else {
            // No postprocessing
            decodedPayload = new String(binaryPayload);
        }

        // Add prefix and suffix
        decodedPayload = String.join("", prefix, decodedPayload, suffix);

        return Optional.of(decodedPayload);
    }

    private GseOpenGps10Byte parseGseOpenGps10BytePayload(byte[] payload) {
        JBBPBitInputStream jbbpBitInputStream = new JBBPBitInputStream(new ByteArrayInputStream(payload), JBBPBitOrder.MSB0);
        return Try.of(() -> getParser().parse(jbbpBitInputStream).mapTo(new GseOpenGps10Byte())).get();
    }

    private JBBPParser getParser() {
        return JBBPParser.prepare(
                "varlong:8 MessageBlockType;"
                        + "varlong:3 MagicNumber;"
                        + "varlong:23 Longitude;"
                        + "varlong:6 Heading;"
                        + "varlong:10 Time;"
                        + "varlong:22 Latitude;"
                        + "varlong:6 Speed;"
                        + "varlong:10 Altitude;",
                JBBPBitOrder.MSB0,
                new VarLongCustomTypeProcessor(),
                0
        );
    }

    public abstract String getUuid();
}
