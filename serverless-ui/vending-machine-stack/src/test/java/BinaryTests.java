import awslabs.client.ssm.AcknowledgeContent;
import awslabs.client.ssm.ClientMessage;
import awslabs.client.ssm.ClientMessageType;
import com.awslabs.general.helpers.implementations.JacksonHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BinaryTests {
    private final String messageSent1String = "00000074000000000000000000000000000000696e7075745f73747265616d5f646174610000000100000000828cc3df00000000000000000000000000000001c679a7126e3043e5b6c7371b09d8665d643766333637333238336632326434373332313062373366393635643565333700000003000000167b22636f6c73223a3133372c22726f7773223a31347d";
    private final ClientMessage messageSent1 = ClientMessage.from(messageSent1String);
    private final String messageReceived1String = "0000007461636b6e6f776c65646765202020202020202020202020202020202020202020000000010000017a828cc4610000000000000000000000000000000394c6a3d5ad34c776d2d45340775144b67d52fcbf81b74a464d63053686c1e87f893f6053ce4d90c5b8cf2d7f714e448100000000000000af7b2241636b6e6f776c65646765644d65737361676554797065223a22696e7075745f73747265616d5f64617461222c2241636b6e6f776c65646765644d6573736167654964223a2262366337333731622d303964382d363635642d633637392d613731323665333034336535222c2241636b6e6f776c65646765644d65737361676553657175656e63654e756d626572223a302c22497353657175656e7469616c4d657373616765223a747275657d";
    private final ClientMessage messageReceived1 = ClientMessage.from(messageReceived1String);
    private final String messageReceived2String = "000000746f75747075745f73747265616d5f646174612020202020202020202020202020000000010000017a828cc63200000000000000000000000000000001ac64a630cf2be4f9d4e86282ef504949e5b111ba26d5bb1cc89d8a3d74fad4cf1ebd5701c29877969c322b456d0784a300000001000000022420";
    private final ClientMessage messageReceived2 = ClientMessage.from(messageReceived2String);
    private final String messageSent2String = "0000007400000000000000000000000000000000000000000061636b6e6f776c656467650000000100000000828cc5ca00000000000000000000000000000001a4ed35c5458a4e20ac14cd5672099eba633739633339623836626365643331336263393866366139323632313339323800000001000000b07b2241636b6e6f776c65646765644d65737361676554797065223a226f75747075745f73747265616d5f64617461222c2241636b6e6f776c65646765644d6573736167654964223a2264346538363238322d656635302d343934392d616336342d613633306366326265346639222c2241636b6e6f776c65646765644d65737361676553657175656e63654e756d626572223a302c22497353657175656e7469616c4d657373616765223a747275657d";
    private final ClientMessage messageSent2 = ClientMessage.from(messageSent2String);
    private final String messageSent3String = "00000074000000000000000000000000000000696e7075745f73747265616d5f646174610000000100000000828dbf59000000000000000100000000000000003fda0a1a6fad450492cd073d85a48d0c396431653065326439343539643036353233616431336532386134303933633200000001000000010d";
    private final ClientMessage messageSent3 = ClientMessage.from(messageSent3String);
    private final AcknowledgeContent acknowledgeContent1 = JacksonHelper.tryParseJson(messageReceived1.payloadString, AcknowledgeContent.class).get();
    private final AcknowledgeContent acknowledgeContent2 = JacksonHelper.tryParseJson(messageSent2.payloadString, AcknowledgeContent.class).get();

    @Test
    public void shouldConvertMessagesBetweenFormats() {
        assertThat(messageSent1String, is(messageSent1.toHexString()));
        assertThat(messageReceived1String, is(messageReceived1.toHexString()));
        assertThat(messageReceived2String, is(messageReceived2.toHexString()));
        assertThat(messageSent2String, is(messageSent2.toHexString()));
        assertThat(messageSent3String, is(messageSent3.toHexString()));
    }

    @Test
    public void shouldParseAcknowledgeContentJsonProperly() {
        assertThat(acknowledgeContent1.AcknowledgedMessageType, is("input_stream_data"));
        assertThat(acknowledgeContent1.AcknowledgedMessageId, is("b6c7371b-09d8-665d-c679-a7126e3043e5"));
        assertThat(acknowledgeContent1.AcknowledgedMessageSequenceNumber, is(0L));
        assertThat(acknowledgeContent1.IsSequentialMessage, is(true));
    }

    @Test
    public void shouldGenerateCorrectUuidForAcknowledgeContent() {
        String messageReceived2Uuid = AcknowledgeContent.messageIdToUuidString(messageReceived2.messageId);

        assertThat(messageReceived2Uuid, is(acknowledgeContent2.AcknowledgedMessageId));
    }

    @Test
    public void shouldGetCorrectMessageType() {
        ClientMessage messageSent1 = ClientMessage.from(messageSent1String);
        assertThat(ClientMessageType.from(messageSent1).get(), is(ClientMessageType.INPUT_STREAM_DATA));
        assertThat(messageSent1.payloadType, is(3));
        ClientMessage messageReceived1 = ClientMessage.from(messageReceived1String);
        assertThat(ClientMessageType.from(messageReceived1).get(), is(ClientMessageType.ACKNOWLEDGE));
        assertThat(messageReceived1.payloadType, is(0));
        ClientMessage messageReceived2 = ClientMessage.from(messageReceived2String);
        assertThat(ClientMessageType.from(messageReceived2).get(), is(ClientMessageType.OUTPUT_STREAM_DATA));
        assertThat(messageReceived2.payloadType, is(1));
        ClientMessage messageSent2 = ClientMessage.from(messageSent2String);
        assertThat(ClientMessageType.from(messageSent2).get(), is(ClientMessageType.ACKNOWLEDGE));
        assertThat(messageSent2.payloadType, is(1));
        ClientMessage messageSent3 = ClientMessage.from(messageSent3String);
        assertThat(ClientMessageType.from(messageSent3).get(), is(ClientMessageType.INPUT_STREAM_DATA));
        assertThat(messageSent2.payloadType, is(1));
    }

    @Test
    public void debug() {
        String sendLMessage = "000000746f75747075745f73747265616d5f646174612020202020202020202020202020000000010000017aa088d854000000000000000100000000000000008f4f4272a13b383fc45a5aa0754a4e05acac86c0e609ca906f632b0e2dacccb2b77d22b0621f20ebece1a4835b93f6f000000001000000016c";
        String receiveACKMessage = "0000007400000000000000000000000000000000000000000061636b6e6f776c656467650000000100000000a088d89500000000000000010000000000000000c867ec02af2e4ad29aeaf29e0f9922a2626430656331643862386436333139303261623764306465613033393732633400000001000000b07b2241636b6e6f776c65646765644d65737361676554797065223a226f75747075745f73747265616d5f64617461222c2241636b6e6f776c65646765644d6573736167654964223a2263343561356161302d373534612d346530352d386634662d343237326131336233383366222c2241636b6e6f776c65646765644d65737361676553657175656e63654e756d626572223a312c22497353657175656e7469616c4d657373616765223a747275657d";
        String receiveLMessage = "00000074000000000000000000000000000000696e7075745f73747265616d5f646174610000000100000000a08a38d2000000000000000200000000000000008b2d78af43ca449585d36ddf49c31723613431323866626138643430326635386663336134343466323735373936303700000003000000167b22636f6c73223a3132362c22726f7773223a31337d";
        String sendLACKMessage = "0000007461636b6e6f776c65646765202020202020202020202020202020202020202020000000010000017aa08a38ab00000000000000000000000000000003b8df839c1bf9548ef746b651a7984dc5bb421de1389b14e77b8e664adfad6741b8ef0f2da418eddd2a3452b46aafe76c00000000000000af7b2241636b6e6f776c65646765644d65737361676554797065223a22696e7075745f73747265616d5f64617461222c2241636b6e6f776c65646765644d6573736167654964223a2238356433366464662d343963332d313732332d386232642d373861663433636134343935222c2241636b6e6f776c65646765644d65737361676553657175656e63654e756d626572223a322c22497353657175656e7469616c4d657373616765223a747275657d";

        ClientMessage sendL = ClientMessage.from(sendLMessage);
        ClientMessage receiveACK = ClientMessage.from(receiveACKMessage);
        ClientMessage receiveL = ClientMessage.from(receiveLMessage);
        ClientMessage sendLACK = ClientMessage.from(sendLACKMessage);

        int a = 5;

        byte[] asdf = new byte[16];
        int pos = 0;
        asdf[pos++] = -117;
        asdf[pos++] = 45;
        asdf[pos++] = 120;
        asdf[pos++] = -81;
        asdf[pos++] = 67;
        asdf[pos++] = -54;
        asdf[pos++] = 68;
        asdf[pos++] = -107;
        asdf[pos++] = -123;
        asdf[pos++] = -45;
        asdf[pos++] = 109;
        asdf[pos++] = -33;
        asdf[pos++] = 73;
        asdf[pos++] = -61;
        asdf[pos++] = 23;
        asdf[pos++] = 35;

        String ackUuid = AcknowledgeContent.messageIdToUuidString(asdf);
        ackUuid = null;
    }
}
