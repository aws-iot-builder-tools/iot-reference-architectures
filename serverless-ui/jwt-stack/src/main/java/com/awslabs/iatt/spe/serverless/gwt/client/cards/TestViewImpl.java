package com.awslabs.iatt.spe.serverless.gwt.client.cards;

import com.awslabs.iatt.spe.serverless.gwt.client.components.CodeCard;
import com.awslabs.iatt.spe.serverless.gwt.client.events.AttributionData;
import com.awslabs.iatt.spe.serverless.gwt.client.events.AuthorizerName;
import com.awslabs.iatt.spe.serverless.gwt.client.mqtt.MqttClient;
import com.awslabs.iatt.spe.serverless.gwt.client.shared.JwtResponse;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import elemental2.dom.HTMLDivElement;
import org.dominokit.domino.api.client.annotations.UiView;
import org.dominokit.domino.ui.cards.Card;
import org.dominokit.domino.ui.grid.Column;
import org.dominokit.domino.ui.grid.Row;
import org.dominokit.domino.ui.lists.ListGroup;
import org.dominokit.domino.view.BaseElementView;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.awslabs.iatt.spe.serverless.gwt.client.SharedWithServer.topicMqttWildcard;
import static com.awslabs.iatt.spe.serverless.gwt.client.shared.Helpers.getTokenWithSignature;
import static com.awslabs.iatt.spe.serverless.gwt.client.shared.JwtResponse.TOKEN_KEY_NAME;
import static org.jboss.elemento.Elements.h;

@UiView(presentable = TestProxy.class)
public class TestViewImpl extends BaseElementView<HTMLDivElement> implements TestView {
    public static final String X_AMZ_CUSTOMAUTHORIZER_NAME = "x-amz-customauthorizer-name";
    public static final String X_AMZ_CUSTOMAUTHORIZER_SIGNATURE = "x-amz-customauthorizer-signature";
    @NonNls
    public static final String GET_CACERT_VIA_CURL = "<(curl https://www.amazontrust.com/repository/AmazonRootCA1.pem)";
    public static final String PLATFORM = "Platform";
    public static final String BLANK = "";
    private MqttClient mqttClient;
    private ListGroup<Map.Entry<String, String>> messages;
    private List<Map.Entry<String, String>> messageList;
    private List<CodeCard> parameterList;
    private Card mqttBufferCard;
    private Card mqttParametersCard;
    private ListGroup<CodeCard> parameters;
    private CodeCard mosquittoCommandCard;
    private CodeCard curlCommandCard;
    private CodeCard testInvokeWithMqttContextCommandCard;
    private CodeCard testInvokeWithHttpContextCommandCard;
    private CodeCard testInvokeWithSignatureVerificationCommandCard;
    private Optional<AuthorizerName> optionalAuthorizerName = Optional.empty();
    private Optional<AttributionData> optionalAttributionData = Optional.empty();
    private Optional<JwtResponse> optionalJwtResponse = Optional.empty();
    private TestUiHandlers uiHandlers;
    private CodeCard jsonCard;
    private CodeCard hostCard;
    private CodeCard usernameCard;
    private CodeCard topicCard;
    private CodeCard clientIdCard;

    private static String uriEscape(String input) {
        return input.replaceAll("\\+", "%2B").replaceAll("\\/", "%2F");
    }

    @Override
    protected HTMLDivElement init() {
        uiHandlers.getAuthorizerName();
        uiHandlers.getMqttClient();

        buildMosquittoCommandCard();
        buildCurlCommandCard();
        buildTestInvokeWithMqttContextCard();
        buildTestInvokeWithHttpContextCard();
        buildTestInvokeWithSignatureVerificationCommandCard();
        getMessagesListGroup();
        buildMqttBufferCard();
        buildMqttParametersCard();

        return Card.create("Test", "You can test JWTs on this tab")
                .appendChild(mqttBufferCard)
                .appendChild(mqttParametersCard)
                .appendChild(mosquittoCommandCard)
                .appendChild(curlCommandCard)
                .appendChild(testInvokeWithMqttContextCommandCard)
                .appendChild(testInvokeWithHttpContextCommandCard)
                .appendChild(testInvokeWithSignatureVerificationCommandCard)
                .element();
    }

    // Card creation section START

    private void getMessagesListGroup() {
        messages = ListGroup.create();
        messageList = new ArrayList<>();

        messages.setItemRenderer((listGroup, item) -> {
            Map.Entry<String, String> entry = item.getValue();
            String topic = entry.getKey();
            String payload = entry.getValue();
            item.setSelectable(false);
            item.appendChild(Row.create()
                    .appendChild(Column.span2()
                            .appendChild(h(6).textContent(topic))
                            .appendChild(h(6).textContent(payload))
                    )
            );
        });
    }

    private void buildMosquittoCommandCard() {
        mosquittoCommandCard = CodeCard.createCodeCard(BLANK)
                .setTitle("Mosquitto publish command");

        invalidateMosquitoCommandCard();
    }

    private void buildCurlCommandCard() {
        curlCommandCard = CodeCard.createCodeCard(BLANK)
                .setTitle("Curl publish command");

        invalidateCurlCommandCard();
    }

    private void buildMqttBufferCard() {
        mqttBufferCard = Card.create("Messages", "Live messages will show up here")
                .appendChild(messages);
    }

    private void buildMqttParametersCard() {
        jsonCard = new CodeCard().setTitle("JSON config");
        hostCard = new CodeCard().setTitle("Host");
        usernameCard = new CodeCard().setTitle("Username");
        topicCard = new CodeCard().setTitle("Topic");
        clientIdCard = new CodeCard().setTitle("Client ID");

        mqttParametersCard = Card.create("Parameters", "Use this to configure a third party MQTT client")
                .appendChild(jsonCard)
                .appendChild(hostCard)
                .appendChild(usernameCard)
                .appendChild(topicCard)
                .appendChild(clientIdCard)
                .setCollapsible()
                .collapse();

        invalidateMqttParameters();
    }

    private void buildTestInvokeWithMqttContextCard() {
        testInvokeWithMqttContextCommandCard = CodeCard.createCodeCard(BLANK)
                .setTitle("Test invoke with MQTT context command");

        invalidateTestInvokeWithMqttContextCommandLine();
    }

    private void buildTestInvokeWithHttpContextCard() {
        testInvokeWithHttpContextCommandCard = CodeCard.createCodeCard(BLANK)
                .setTitle("Test invoke with HTTP context command");

        invalidateTestInvokeWithHttpContextCommandLine();
    }

    // Card creation section END

    // Card update section START

    private void buildTestInvokeWithSignatureVerificationCommandCard() {
        testInvokeWithSignatureVerificationCommandCard = CodeCard.createCodeCard(BLANK)
                .setTitle("Test invoke with signature verification command");

        invalidateTestInvokeWithSignatureVerificationCommandLine();
    }

    private void updateMosquittoCommandLine() {
        if (!optionalAuthorizerName.isPresent()) {
            // Can't update this without the authorizer name
            return;
        }

        if (!optionalJwtResponse.isPresent()) {
            // Can't update this without the JWT response
            return;
        }

        JwtResponse jwtResponse = optionalJwtResponse.get();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bash -c \"mosquitto_pub -d --tls-alpn mqtt \\\n");
        stringBuilder.append("  -h " + jwtResponse.endpoint + " \\\n");
        stringBuilder.append("  -p 443 \\\n");
        stringBuilder.append("  -i " + jwtResponse.iccid + " \\\n");
        stringBuilder.append("  -t clients/jwt/" + jwtResponse.iccid + " \\\n");
        stringBuilder.append("  -m 'Message from " + jwtResponse.iccid + " via MQTTS' \\\n");

        stringBuilder.append("  -u '");

        String username = getMqttAndCurlUsernameField(jwtResponse);

        if (jwtResponse.signatureAutoValidated) {
            // When the signature is validated AWS IoT expects a query string so we need to prefix it with a question mark
            stringBuilder.append("?");
        } else {
            // The username field will be a query string after the username. Convert the first ampersand after the token to a question mark.
            username = username.replaceFirst("&", "?");
        }

        stringBuilder.append(username);
        stringBuilder.append("' \\\n");
        stringBuilder.append("  --cafile " + GET_CACERT_VIA_CURL + "\"");

        mosquittoCommandCard.setCode(stringBuilder.toString());
    }

    private void updateCurlCommandLine() {
        if (!optionalAuthorizerName.isPresent()) {
            // Can't update this without the authorizer name
            return;
        }

        if (!optionalJwtResponse.isPresent()) {
            // Can't update this without the JWT response
            return;
        }

        JwtResponse jwtResponse = optionalJwtResponse.get();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bash -c \"curl -X POST \\\n");

        String url = "https://" + jwtResponse.endpoint + "/topics/clients/jwt/" + jwtResponse.iccid + "?";
        url += getMqttAndCurlUsernameField(jwtResponse);

        // Escape ampersands otherwise bash tries to run a partial command in the background
        url = url.replace("&", "\\&");

        stringBuilder.append(url);
        stringBuilder.append("\\\n");
        stringBuilder.append("  --data 'Message from " + jwtResponse.iccid + " via HTTPS' \\\n");
        stringBuilder.append("  --cacert " + GET_CACERT_VIA_CURL + "\"");

        curlCommandCard.setCode(stringBuilder.toString());
    }

    private void updateTestInvokeWithMqttContextCommandLine() {
        if (!optionalAuthorizerName.isPresent()) {
            // Can't update this without the authorizer name
            return;
        }

        if (!optionalJwtResponse.isPresent()) {
            // Can't update this without the JWT response
            return;
        }

        JwtResponse jwtResponse = optionalJwtResponse.get();

        JSONObject mqttContext = new JSONObject();

        String usernameString = getMqttAndCurlUsernameField(jwtResponse);

        if (jwtResponse.signatureAutoValidated) {
            // When the signature is validated AWS IoT expects a query string so we need to prefix it with a question mark
            usernameString = "?" + usernameString;
        } else {
            // The username field will be a query string after the username. Convert the first ampersand after the token to a question mark.
            usernameString = usernameString.replaceFirst("&", "?");
        }

        JSONString usernameValue = new JSONString(usernameString);

        mqttContext.put("username", usernameValue);
        mqttContext.put("clientId", new JSONString(jwtResponse.iccid));

        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("aws iot test-invoke-authorizer --mqtt-context \"");
        // This value needs to be escaped since it is in double-quotes. We use double-quotes so that wrapString doesn't break the command.
        commandStringBuilder.append(mqttContext.toString().replace("\"", "\\\""));
        commandStringBuilder.append("\"");

        commandStringBuilder.append(" --authorizer-name ");
        commandStringBuilder.append(optionalAuthorizerName.get().value);

        testInvokeWithMqttContextCommandCard.setCode(commandStringBuilder.toString());
    }

    @NotNull
    private String removeTokenKeyNamePrefix(String tempString) {
        return tempString.replaceFirst(TOKEN_KEY_NAME + "=", "");
    }

    private void updateTestInvokeWithHttpContextCommandLine() {
        if (!optionalAuthorizerName.isPresent()) {
            // Can't update this without the authorizer name
            return;
        }

        if (!optionalJwtResponse.isPresent()) {
            // Can't update this without the JWT response
            return;
        }

        JwtResponse jwtResponse = optionalJwtResponse.get();

        // Don't call getMqttAndCurlUsernameField since HTTPS always needs the token key name to be present
        String queryString = "?" + getString(jwtResponse);

        JSONObject httpContext = new JSONObject();
        httpContext.put("headers", new JSONObject());
        httpContext.put("queryString", new JSONString(queryString));

        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("aws iot test-invoke-authorizer --http-context \"");

        // This value needs to be escaped since it is in double-quotes. We use double-quotes so that wrapString doesn't break the command.
        commandStringBuilder.append(httpContext.toString().replace("\"", "\\\""));

        commandStringBuilder.append("\"");

        commandStringBuilder.append(" --authorizer-name ");
        commandStringBuilder.append(optionalAuthorizerName.get().value);

        testInvokeWithHttpContextCommandCard.setCode(commandStringBuilder.toString());
    }

    // Card update section END

    private void updateTestInvokeWithSignatureVerificationCommandLine() {
        if (!optionalAuthorizerName.isPresent()) {
            // Can't update this without the authorizer name
            return;
        }

        if (!optionalJwtResponse.isPresent()) {
            // Can't update this without the JWT response
            return;
        }

        JwtResponse jwtResponse = optionalJwtResponse.get();

        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("aws iot test-invoke-authorizer --token ");

        if (jwtResponse.signatureAutoValidated) {
            commandStringBuilder.append(jwtResponse.token);
            commandStringBuilder.append(" --token-signature ");
            commandStringBuilder.append(jwtResponse.signature);
        } else {
            commandStringBuilder.append(getTokenWithSignature(jwtResponse));
        }

        commandStringBuilder.append(" --authorizer-name ");
        commandStringBuilder.append(optionalAuthorizerName.get().value);

        testInvokeWithSignatureVerificationCommandCard.setCode(commandStringBuilder.toString());
    }

    @NotNull
    private String getMqttAndCurlUsernameField(JwtResponse jwtResponse) {
        if (!jwtResponse.signatureAutoValidated) {
            // Signature isn't automatically validated, remove the prefix so it can be passed as the username
            return removeTokenKeyNamePrefix(getString(jwtResponse));
        }

        // Signature is validated, leave the string as is
        return getString(jwtResponse);
    }

    @NotNull
    private Map<String, String> getFieldMap(JwtResponse jwtResponse) {
        Map<String, String> fields = new HashMap<>();
        fields.put(X_AMZ_CUSTOMAUTHORIZER_NAME, optionalAuthorizerName.get().value);

        if (!jwtResponse.signatureAutoValidated) {
            // Use the combined token and signature as the token value
            fields.put(TOKEN_KEY_NAME, getTokenWithSignature(jwtResponse));
        } else {
            // Separate the token and signature
            fields.put(TOKEN_KEY_NAME, jwtResponse.token);
            fields.put(X_AMZ_CUSTOMAUTHORIZER_SIGNATURE, uriEscape(jwtResponse.signature));
        }

        if (optionalAttributionData.map(attributionData -> attributionData.attributionEnabled).orElse(false)) {
            AttributionData attributionData = optionalAttributionData.get();

            String platformValue = String.join(" ", "APN/1", attributionData.partnerName);

            if (!attributionData.solutionName.isEmpty()) {
                platformValue = String.join(",", platformValue, attributionData.solutionName);
            }

            if (!attributionData.versionName.isEmpty()) {
                platformValue = String.join(",", platformValue, attributionData.versionName);
            }

            fields.put(PLATFORM, platformValue);
        }

        return fields;
    }

    private String fieldMapToString(Map<String, String> fieldMap) {
        List<String> fieldList = new ArrayList<>();

        // Add the token first
        fieldList.add(TOKEN_KEY_NAME + "=" + fieldMap.remove(TOKEN_KEY_NAME));

        // Add the rest of the fields
        fieldMap.forEach((key, value) -> fieldList.add(key + "=" + value));

        // Separate the fields with ampersands
        return String.join("&", fieldList);
    }

    private String getString(JwtResponse jwtResponse) {
        return fieldMapToString(getFieldMap(jwtResponse));
    }

    private void invalidate() {
        invalidateMosquitoCommandCard();
        invalidateCurlCommandCard();
        invalidateTestInvokeWithMqttContextCommandLine();
        invalidateTestInvokeWithHttpContextCommandLine();
        invalidateTestInvokeWithSignatureVerificationCommandLine();
    }

    private void invalidateMosquitoCommandCard() {
        mosquittoCommandCard.setCode("Not generated yet");
    }

    private void invalidateCurlCommandCard() {
        curlCommandCard.setCode("Not generated yet");
    }

    private void invalidateTestInvokeWithMqttContextCommandLine() {
        testInvokeWithMqttContextCommandCard.setCode("Not generated yet");
    }

    private void invalidateTestInvokeWithHttpContextCommandLine() {
        testInvokeWithHttpContextCommandCard.setCode("Not generated yet");
    }

    private void invalidateTestInvokeWithSignatureVerificationCommandLine() {
        testInvokeWithSignatureVerificationCommandCard.setCode("Not generated yet");
    }

    private void invalidateMqttParameters() {
        jsonCard.setCode("{}");
        hostCard.setCode("N/A");
        usernameCard.setCode("N/A");
        topicCard.setCode("N/A");
        clientIdCard.setCode("N/A");
    }

    private void addRowAndUpdate(String topic, Object payload) {
        String payloadString = payload.toString();

        messageList.add(0, new AbstractMap.SimpleEntry<>(topic, payloadString));

        while (messageList.size() > 7) {
            messageList.remove(messageList.size() - 1);
        }

        messages.setItems(messageList);
    }

    @Override
    public void onJwtChanged(JwtResponse jwtResponse) {
        this.optionalJwtResponse = Optional.of(jwtResponse);

        updateAll();
    }

    @Override
    public void onAttributionChanged(AttributionData attributionData) {
        this.optionalAttributionData = Optional.empty();

        if (attributionData.attributionEnabled) {
            this.optionalAttributionData = Optional.of(attributionData);
        }

        updateAll();
    }

    private void updateAll() {
        invalidate();

        updateMosquittoCommandLine();
        updateCurlCommandLine();
        updateTestInvokeWithMqttContextCommandLine();
        updateTestInvokeWithHttpContextCommandLine();
        updateMqttParameters();

        if (optionalJwtResponse.isPresent()) {
            JwtResponse jwtResponse = optionalJwtResponse.get();

            if (!jwtResponse.signatureAutoValidated) {
                testInvokeWithSignatureVerificationCommandCard.hide();
            } else {
                updateTestInvokeWithSignatureVerificationCommandLine();
                testInvokeWithSignatureVerificationCommandCard.show();
            }
        }
    }

    private void updateMqttParameters() {
        if (!optionalAuthorizerName.isPresent()) {
            // Can't update this without the authorizer name
            return;
        }

        if (!optionalJwtResponse.isPresent()) {
            // Can't update this without the JWT response
            return;
        }

        JwtResponse jwtResponse = optionalJwtResponse.get();

        hostCard.setCode(jwtResponse.endpoint);
        clientIdCard.setCode(jwtResponse.iccid);
        usernameCard.setCode("?" + getMqttAndCurlUsernameField(jwtResponse));
        topicCard.setCode("clients/jwt/" + jwtResponse.iccid);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("USERNAME", new JSONString(usernameCard.getCode()));
        jsonObject.put("CLIENT_ID", new JSONString(clientIdCard.getCode()));
        jsonObject.put("TOPIC", new JSONString(topicCard.getCode()));
        jsonObject.put("HOST", new JSONString(hostCard.getCode()));
        jsonCard.setCode(JsonUtils.stringify(jsonObject.getJavaScriptObject()));
    }

    @Override
    public void onAuthorizerNameUpdated(AuthorizerName authorizerName) {
        optionalAuthorizerName = Optional.of(authorizerName);
    }

    @Override
    public void onInvalidatedEvent() {
        optionalJwtResponse = Optional.empty();

        invalidate();
    }

    @Override
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
        this.mqttClient.subscribe(topicMqttWildcard);
        this.mqttClient.onMessageCallback(this::addRowAndUpdate);

        this.mqttClient.onConnectCallback(() -> addRowAndUpdate("Connected", "..."));
        mqttClient.onReconnectCallback(() -> addRowAndUpdate("Reconnected", "..."));
        mqttClient.onOfflineCallback(() -> addRowAndUpdate("Offline", "..."));
        mqttClient.onErrorCallback(error -> addRowAndUpdate("Error", error));
    }

    @Override
    public void setUiHandlers(TestUiHandlers uiHandlers) {
        this.uiHandlers = uiHandlers;
    }
}
