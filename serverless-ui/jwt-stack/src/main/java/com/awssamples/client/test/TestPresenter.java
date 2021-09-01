package com.awssamples.client.test;

import com.awssamples.client.events.AttributionChanged;
import com.awssamples.client.events.JwtChanged;
import com.awssamples.client.events.MqttMessage;
import com.awssamples.client.events.RegionDetected;
import com.awssamples.client.place.NameTokens;
import com.awssamples.client.shared.JwtCreationResponse;
import com.awssamples.client.shared.JwtService;
import com.awssamples.client.shared.JwtServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.awssamples.client.shared.Helpers.getTokenWithSignature;
import static com.awssamples.client.shared.JwtCreationResponse.TOKEN_KEY_NAME;

public class TestPresenter implements ITestPresenter {
    public static final String X_AMZ_CUSTOMAUTHORIZER_NAME = "x-amz-customauthorizer-name";
    public static final String X_AMZ_CUSTOMAUTHORIZER_SIGNATURE = "x-amz-customauthorizer-signature";
    public static final String GET_CACERT_VIA_CURL = "<(curl https://www.amazontrust.com/repository/AmazonRootCA1.pem)";
    public static final String PLATFORM = "Platform";
    public static final String BLANK = "";

    public static final String CONTINUE_ON_NEXT_LINE = " \\\n ";
    @Inject
    EventBus eventBus;

    @Inject
    ITestView testView;

    @Inject
    Logger log;
    private Option<String> attributionStringOption = Option.none();
    private Option<String> authorizerNameOption = Option.none();
    public static final JwtServiceAsync JWT_SERVICE_ASYNC = GWT.create(JwtService.class);
    private JwtCreationResponse jwtCreationResponse;
    private Option<String> regionOption = Option.none();

    @Inject
    TestPresenter() {
    }

    @Inject
    public void setup() {
        JWT_SERVICE_ASYNC.getAuthorizerName(new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                MaterialToast.fireToast("Couldn't get authorizer name, MQTT command and AWS CLI command cards will not update [" + caught.getMessage() + "]");
            }

            @Override
            public void onSuccess(String result) {
                authorizerNameOption = Option.of(result);
            }
        });
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(JwtChanged.TYPE, TestPresenter.this::onJwtChanged);
        eventBus.addHandler(AttributionChanged.TYPE, TestPresenter.this::onAttributionChanged);
        eventBus.addHandler(MqttMessage.TYPE, TestPresenter.this::onMqttMessage);
        eventBus.addHandler(RegionDetected.TYPE, TestPresenter.this::onRegionDetected);
    }

    private void onRegionDetected(RegionDetected.Event event) {
        this.regionOption = Option.of(event.region);
    }

    private void onMqttMessage(MqttMessage.Event event) {
        testView.addMqttMessage(event.topic, event.payload);
    }

    private void onAttributionChanged(AttributionChanged.Event event) {
        attributionStringOption = event.attributionStringOption;

        updatePublishCommands();
    }

    private void onJwtChanged(JwtChanged.Event event) {
        jwtCreationResponse = event.jwtCreationResponse;

        if (authorizerNameOption.isEmpty()) {
            MaterialToast.fireToast("Authorizer name has not been retrieved yet. It may still be loading or it may have failed. Test cards have not been updated.");
            return;
        }

        updateTestAuthorizerCommands();
        updatePublishCommands();
    }

    private void updateTestAuthorizerCommands() {
        updateTestInvokeAuthorizerMqtt();
        updateTestInvokeAuthorizerHttp();
        updateTestInvokeAuthorizerWithSignature();
    }

    private void updatePublishCommands() {
        updateMosquittoPubCommand();
        updateCurlPubCommand();
    }

    private void updateTestInvokeAuthorizerMqtt() {
        JSONObject mqttContext = new JSONObject();

        String usernameString = getMqttAndCurlUsernameField(jwtCreationResponse);

        if (jwtCreationResponse.signatureAutoValidated) {
            // When the signature is validated AWS IoT expects a query string so we need to prefix it with a question mark
            usernameString = "?" + usernameString;
        } else {
            // The username field will be a query string after the username. Convert the first ampersand after the token to a question mark.
            usernameString = usernameString.replaceFirst("&", "?");
        }

        JSONString usernameValue = new JSONString(usernameString);

        mqttContext.put("username", usernameValue);
        mqttContext.put("clientId", new JSONString(jwtCreationResponse.iccid));

        StringBuilder commandStringBuilder = new StringBuilder();
        regionOption.map(region -> commandStringBuilder.append(String.join("", "REGION=" + region + " ")));
        commandStringBuilder.append("aws iot test-invoke-authorizer --mqtt-context \"");
        // This value needs to be escaped since it is in double-quotes. We use double-quotes so that wrapString doesn't break the command.
        commandStringBuilder.append(mqttContext.toString().replace("\"", "\\\""));
        commandStringBuilder.append("\"");

        commandStringBuilder.append(" --authorizer-name ");
        commandStringBuilder.append(authorizerNameOption.get());

        testView.updateTestInvokeAuthorizerMqtt(commandStringBuilder.toString());
    }

    private void updateTestInvokeAuthorizerHttp() {
        // Don't call getMqttAndCurlUsernameField since HTTPS always needs the token key name to be present
        String queryString = "?" + getString(jwtCreationResponse);

        JSONObject httpContext = new JSONObject();
        httpContext.put("headers", new JSONObject());
        httpContext.put("queryString", new JSONString(queryString));

        StringBuilder commandStringBuilder = new StringBuilder();
        regionOption.map(region -> commandStringBuilder.append(String.join("", "REGION=" + region + " ")));
        commandStringBuilder.append("aws iot test-invoke-authorizer --http-context \"");

        // This value needs to be escaped since it is in double-quotes. We use double-quotes so that wrapString doesn't break the command.
        commandStringBuilder.append(httpContext.toString().replace("\"", "\\\""));

        commandStringBuilder.append("\"");

        commandStringBuilder.append(" --authorizer-name ");
        commandStringBuilder.append(authorizerNameOption.get());

        testView.updateTestInvokeAuthorizerHttp(commandStringBuilder.toString());
    }

    private void updateTestInvokeAuthorizerWithSignature() {
        StringBuilder commandStringBuilder = new StringBuilder();
        regionOption.map(region -> commandStringBuilder.append(String.join("", "REGION=" + region + " ")));
        commandStringBuilder.append("aws iot test-invoke-authorizer --token ");

        if (jwtCreationResponse.signatureAutoValidated) {
            commandStringBuilder.append(jwtCreationResponse.token);
            commandStringBuilder.append(" --token-signature ");
            commandStringBuilder.append(jwtCreationResponse.signature);
        } else {
            commandStringBuilder.append(getTokenWithSignature(jwtCreationResponse));
        }

        commandStringBuilder.append(" --authorizer-name ");
        commandStringBuilder.append(authorizerNameOption.get());

        testView.updateTestInvokeAuthorizerWithSignature(commandStringBuilder.toString());
    }

    private void updateMosquittoPubCommand() {
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("bash -c \"mosquitto_pub -d --tls-alpn mqtt");
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append(String.join(" ", "  -h", jwtCreationResponse.endpoint));
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append("  -p 443");
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append(String.join(" ", "  -i", jwtCreationResponse.iccid));
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append(String.join("", "  -t clients/jwt/", jwtCreationResponse.iccid));
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append(String.join(" ", "  -m 'Message from", jwtCreationResponse.iccid, "via MQTTS'"));
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);

        commandStringBuilder.append("  -u '");

        String username = getMqttAndCurlUsernameField(jwtCreationResponse);

        if (jwtCreationResponse.signatureAutoValidated) {
            // When the signature is validated AWS IoT expects a query string so we need to prefix it with a question mark
            commandStringBuilder.append("?");
        } else {
            // The username field will be a query string after the username. Convert the first ampersand after the token to a question mark.
            username = username.replaceFirst("&", "?");
        }

        commandStringBuilder.append(username);
        commandStringBuilder.append("' ");
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append(String.join("", "  --cafile ", GET_CACERT_VIA_CURL, "\""));

        testView.updateMosquittoPubCommand(commandStringBuilder.toString());
    }

    private void updateCurlPubCommand() {
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append("bash -c \"curl -X POST");
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);

        String url = "https://" + jwtCreationResponse.endpoint + "/topics/clients/jwt/" + jwtCreationResponse.iccid + "?";
        url += getMqttAndCurlUsernameField(jwtCreationResponse).replace(" ", "+");

        // Escape ampersands otherwise bash tries to run a partial command in the background
        url = url.replace("&", "\\&");

        commandStringBuilder.append(url);
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append(String.join(" ", "  --data 'Message from", jwtCreationResponse.iccid, "via HTTPS'"));
        commandStringBuilder.append(CONTINUE_ON_NEXT_LINE);
        commandStringBuilder.append("  --cacert " + GET_CACERT_VIA_CURL + "\"");

        testView.updateCurlPubCommand(commandStringBuilder.toString());
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Widget getWidget() {
        return testView.getWidget();
    }

    @Override
    public String getToken() {
        return NameTokens.test();
    }

    private String getMqttAndCurlUsernameField(JwtCreationResponse jwtCreationResponse) {
        if (!jwtCreationResponse.signatureAutoValidated) {
            // Signature isn't automatically validated, remove the prefix so it can be passed as the username
            return removeTokenKeyNamePrefix(getString(jwtCreationResponse));
        }

        // Signature is validated, leave the string as is
        return getString(jwtCreationResponse);
    }

    private String removeTokenKeyNamePrefix(String tempString) {
        return tempString.replaceFirst(TOKEN_KEY_NAME + "=", "");
    }

    private String getString(JwtCreationResponse jwtCreationResponse) {
        return fieldMapToString(getFieldMap(jwtCreationResponse));
    }

    private Map<String, String> getFieldMap(JwtCreationResponse jwtCreationResponse) {
        Map<String, String> fields = new HashMap<>();
        fields.put(X_AMZ_CUSTOMAUTHORIZER_NAME, authorizerNameOption.get());

        if (!jwtCreationResponse.signatureAutoValidated) {
            // Use the combined token and signature as the token value
            fields.put(TOKEN_KEY_NAME, getTokenWithSignature(jwtCreationResponse));
        } else {
            // Separate the token and signature
            fields.put(TOKEN_KEY_NAME, jwtCreationResponse.token);
            fields.put(X_AMZ_CUSTOMAUTHORIZER_SIGNATURE, uriEscape(jwtCreationResponse.signature));
        }

        attributionStringOption.map(attributionString -> fields.put(PLATFORM, attributionString));

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

    private static String uriEscape(String input) {
        return input.replaceAll("\\+", "%2B").replaceAll("\\/", "%2F");
    }
}
