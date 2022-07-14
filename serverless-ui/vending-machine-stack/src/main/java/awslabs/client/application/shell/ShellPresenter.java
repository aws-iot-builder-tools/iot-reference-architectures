/*
 * #%L
 * GwtMaterial
 * %%
 * Copyright (C) 2015 - 2017 GwtMaterialDesign
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package awslabs.client.application.shell;

import awslabs.client.IotService;
import awslabs.client.IotServiceAsync;
import awslabs.client.application.DaggerAppInjector;
import awslabs.client.application.events.*;
import awslabs.client.application.shared.ReceivesEvents;
import awslabs.client.mqtt.AWSIoTData;
import awslabs.client.mqtt.ClientConfig;
import awslabs.client.mqtt.MqttClient;
import awslabs.client.place.NameTokens;
import com.google.gwt.core.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.inject.Inject;

import static awslabs.client.application.shared.GwtHelper.info;

public class ShellPresenter implements EntryPoint, ReceivesEvents, ValueChangeHandler<String> {
    public static final IotServiceAsync IOT_SERVICE_ASYNC = GWT.create(IotService.class);
    public static final String WEBSOCKET_PROTOCOL = "wss";
    public static final String STARTED = "started";
    public static final String FINISHED = "finished";
    public static final String PROGRESS = "progress";
    public static final String VENDINGMACHINE = "vendingmachine";
    public static final String CLIENTS = "clients";
    public static final String SINGLE_LEVEL_WILDCARD = "+";
    public static final String SYSTEM = "system";
    public static final String ACTIVATION_ID = "activationId";
    public static final String USER_ID_COOKIE_NAME = "userId";
    private static final String TOPIC_PREFIX = String.join("/", CLIENTS, VENDINGMACHINE);
    public static Option<String> clientIdOption = Option.none();
    public static Option<String> userIdOption = Option.none();
    @Inject
    EventBus eventBus;
    @Inject
    IShellView applicationView;
    private MqttClient mqttClient;

    @Inject
    public ShellPresenter() {
    }

    public static native void globalVariableSetup() /*-{
        $wnd.AWSIoTData = require('aws-iot-device-sdk');
    }-*/;

    @Inject
    public void setup() {
        ScriptInjector.fromUrl("aws-iot-sdk-browser-bundle-min.js").setCallback(
                new Callback<Void, Exception>() {
                    public void onFailure(Exception reason) {
                        Window.alert("Failed to load the AWS IoT SDK");
                    }

                    public void onSuccess(Void result) {
                        globalVariableSetup();
                        mqttSetup();
                    }
                }).inject();

        // Add history listener
        History.addValueChangeHandler(this);

        // Now that we've setup our listener, fire the initial history state.
        History.fireCurrentHistoryState();
    }

    private void onBuildRequestSuccessful(BuildRequestSuccessful.Event buildRequestSuccessfulEvent) {
        MaterialToast.fireToast("Build requested " + buildRequestSuccessfulEvent.buildId);
    }

    private void mqttSetup() {
        // Close the MQTT client if it exists already
        if (mqttClient != null) {
            mqttClient.end(true);
        }

        mqttClient = null;

        eventBus.fireEvent(new MqttConnectionAttempt.Event());

        // Use the user ID in their cookies, if there is one
        String userId = Option.of(Cookies.getCookie(USER_ID_COOKIE_NAME))
                // If there isn't then just send a NULL
                .getOrNull();

        IOT_SERVICE_ASYNC.getClientConfig(userId,
                new AsyncCallback<ClientConfig>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        eventBus.fireEvent(new MqttConnectionFailed.Event());
                        Window.alert("Couldn't get credentials for an MQTT connection [" + caught.getMessage() + "]");
                    }

                    @Override
                    public void onSuccess(ClientConfig clientConfig) {
                        setUserIdCookie(clientConfig.userId);
                        userIdOption = Option.of(clientConfig.userId);
                        clientIdOption = Option.of(clientConfig.clientId);

                        JSONObject jsonObject = new JSONObject();

                        jsonObject.put("region", new JSONString(clientConfig.region));
                        jsonObject.put("host", new JSONString(clientConfig.endpointAddress));
                        jsonObject.put("clientId", new JSONString(clientConfig.clientId));
                        jsonObject.put("protocol", new JSONString(WEBSOCKET_PROTOCOL));
                        jsonObject.put("maximumReconnectTimeMs", new JSONNumber(8000));
                        jsonObject.put("debug", JSONBoolean.getInstance(true));
                        jsonObject.put("accessKeyId", new JSONString(clientConfig.accessKeyId));
                        jsonObject.put("secretKey", new JSONString(clientConfig.secretAccessKey));
                        jsonObject.put("sessionToken", new JSONString(clientConfig.sessionToken));

                        // NOTE: Don't try to do anything fancy here with Try or Option. Native JavaScript types don't work well with them.
                        mqttClient = AWSIoTData.device(jsonObject.getJavaScriptObject());

                        mqttClient.subscribe(String.join("/", TOPIC_PREFIX, clientIdOption.get(), STARTED));
                        mqttClient.subscribe(String.join("/", TOPIC_PREFIX, clientIdOption.get(), FINISHED));
                        mqttClient.subscribe(String.join("/", TOPIC_PREFIX, clientIdOption.get(), PROGRESS));
                        mqttClient.subscribe(String.join("/", TOPIC_PREFIX, clientIdOption.get(), SYSTEM));

                        eventBus.fireEvent(new MqttConnectionSuccessful.Event(mqttClient));
                    }
                });
    }

    private void setUserIdCookie(String userId) {
        Cookies.setCookie(USER_ID_COOKIE_NAME, userId);
    }

    public void onConnectionAttempt(MqttConnectionAttempt.Event mqttConnectionAttemptEvent) {
        applicationView.getDevBoardWidget().setIconColor(Color.CYAN);
        applicationView.getNavBrand().setText("IoT vending machine (connecting...)");
    }

    public void onConnectionFailed(MqttConnectionFailed.Event mqttConnectionFailedEvent) {
        applicationView.getDevBoardWidget().setIconColor(Color.RED);
        applicationView.getNavBrand().setText("IoT vending machine (could not connect)");
    }

    public void onConnectionSuccessful(MqttConnectionSuccessful.Event mqttConnectionSuccessfulEvent) {
        applicationView.getDevBoardWidget().setIconColor(Color.GREEN);
        mqttConnectionSuccessfulEvent.mqttClient.onMessageCallback(ShellPresenter.this::onMessage);
        applicationView.getNavBrand().setText("IoT vending machine (" + userIdOption.get() + ")");
    }

    public void onMessage(String topic, Object payload) {
        Option<String> payloadStringOption = Option.of(payload)
                .map(Object::toString);

        if (payloadStringOption.isEmpty()) {
            Window.alert("NULL payload on topic: " + topic + ", this is a bug");
            return;
        }

        String payloadString = payloadStringOption.get();

        List<String> splitTopic = List.of(topic.split("/"));

        if (splitTopic.size() != 4) {
            // Ignore this message
            info("message NOT valid [" + topic + "] [" + payloadString + "]");
            return;
        }

        String type = splitTopic.get(3);

        JSONValue jsonValue = JSONParser.parseStrict(payloadString);

        if (handleSystemMessage(jsonValue, type)) return;

        Option<String> buildIdOption = getBuildIdOption(jsonValue);

        if (handleStartedMessage(type, buildIdOption)) return;

        if (handleFinishedMessage(type, jsonValue, buildIdOption)) return;

        if (handleProgressMessage(type, jsonValue, buildIdOption)) return;

        info("unexpected action [" + topic + "] [" + payload + "]");
    }

    private boolean handleProgressMessage(String type, JSONValue jsonValue, Option<String> buildUiOption) {
        if (!type.equals(PROGRESS)) {
            return false;
        }

        Option<String> commentOption = Option.of(jsonValue.isObject())
                .flatMap(map -> Option.of(map.get("comment")))
                .map(JSONValue::isString)
                .map(JSONString::stringValue);

        Option<Integer> currentStepOption = Option.of(jsonValue.isObject())
                .flatMap(map -> Option.of(map.get("currentStep")))
                .map(JSONValue::isNumber)
                .map(JSONNumber::doubleValue)
                .map(Double::intValue);

        Option<Integer> totalStepsOption = Option.of(jsonValue.isObject())
                .flatMap(map -> Option.of(map.get("totalSteps")))
                .map(JSONValue::isNumber)
                .map(JSONNumber::doubleValue)
                .map(Double::intValue);

        Option<Integer> stepProgressOption = Option.of(jsonValue.isObject())
                .flatMap(map -> Option.of(map.get("stepProgress")))
                .map(JSONValue::isNumber)
                .map(JSONNumber::doubleValue)
                .map(Double::intValue);

        String buildId = buildUiOption.getOrElseThrow(() -> new RuntimeException("No build ID, cannot process progress message"));

        BuildProgress.Event buildProgressEvent = new BuildProgress.Event(buildId);
        commentOption.map(buildProgressEvent::comment);
        currentStepOption.map(buildProgressEvent::currentStep);
        totalStepsOption.map(buildProgressEvent::totalSteps);
        stepProgressOption.map(buildProgressEvent::stepProgress);

        eventBus.fireEvent(buildProgressEvent);

        return true;
    }

    private boolean handleFinishedMessage(String type, JSONValue jsonValue, Option<String> buildIdOption) {
        if (!type.equals(FINISHED)) {
            return false;
        }

        Option<String> presignedS3UrlOption = Option.of(jsonValue.isObject())
                .flatMap(map -> Option.of(map.get(FINISHED)))
                .map(JSONValue::isString)
                .map(JSONString::stringValue);

        presignedS3UrlOption.getOrElseThrow(() -> new RuntimeException("No presigned URL, cannot process finished message"));
        String buildId = buildIdOption.getOrElseThrow(() -> new RuntimeException("No build ID, cannot process finished message"));

        eventBus.fireEvent(new BuildFinished.Event(buildId));

        return true;
    }

    private boolean handleStartedMessage(String type, Option<String> optionalBuildId) {
        if (!type.equals(STARTED)) {
            return false;
        }

        String buildId = optionalBuildId.getOrElseThrow(() -> new RuntimeException("No build ID, cannot process started message"));

        eventBus.fireEvent(new BuildStarted.Event(buildId));

        return true;
    }

    private Option<String> getBuildIdOption(JSONValue jsonValue) {
        Option<String> buildIdOption = Option.of(jsonValue.isObject())
                .flatMap(map -> Option.of(map.get("buildId")))
                .map(JSONValue::isString)
                .map(JSONString::stringValue);

        if (buildIdOption.isEmpty()) {
            Window.alert("Build ID missing from the message, this should never happen");
        }

        return buildIdOption;
    }

    private boolean handleSystemMessage(JSONValue jsonValue, String type) {
        if (!SYSTEM.equals(type)) {
            return false;
        }

        Option<String> activationIdOption = Option.of(jsonValue.isObject())
                .flatMap(map -> Option.of(map.get(ACTIVATION_ID)))
                .map(JSONValue::isString)
                .map(JSONString::stringValue);

        if (activationIdOption.isEmpty()) {
            Window.alert("Activation ID missing from a system message, this should never happen");
            return true;
        }

        String activationId = activationIdOption.get();

        SystemCreated.Event systemCreatedEvent = new SystemCreated.Event(activationId);

        eventBus.fireEvent(systemCreatedEvent);

        return true;
    }

    @Override
    public void onModuleLoad() {
        // Must use schedule deferred to make sure everything is loaded before we try to do anything
        // https://gitter.im/GwtMaterialDesign/gwt-material?at=58868073c0de6f017fe11c7d (Marcin Szałomski @baldram Jan 23 2017 17:15)
        Scheduler.get().scheduleDeferred(() -> {
            // Inject the dependencies
            Try.run(() -> DaggerAppInjector.create().inject(this))
                    // Set the application view as the root
                    .andThen(() -> RootPanel.get().add(applicationView.getWidget()))
                    // All injected event receivers are already bound at this point except this class because it can't be injected
                    //   into itself. So we bind this final class here.
                    .andThen(this::bindEventBus)
                    .onFailure(this::logThrowable);
        });
    }

    private void logThrowable(Throwable throwable) {
        info("Exception!");
        info("Exception! " + throwable.getMessage());
        throwable.printStackTrace();
        throw new RuntimeException(throwable);
    }

    private void onUpdatedBuildCount(UpdatedBuildCount.Event updatedBuildCountEvent) {
        applicationView.updateBuildCount(updatedBuildCountEvent.count);
    }

    public void onUpdatedSystemCount(UpdatedSystemCount.Event updatedSystemCountEvent) {
        applicationView.updateSystemCount(updatedSystemCountEvent.count);
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(MqttConnectionAttempt.TYPE, ShellPresenter.this::onConnectionAttempt);
        eventBus.addHandler(MqttConnectionFailed.TYPE, ShellPresenter.this::onConnectionFailed);
        eventBus.addHandler(MqttConnectionSuccessful.TYPE, ShellPresenter.this::onConnectionSuccessful);
        eventBus.addHandler(BuildRequestSuccessful.TYPE, ShellPresenter.this::onBuildRequestSuccessful);
        eventBus.addHandler(UpdatedBuildCount.TYPE, ShellPresenter.this::onUpdatedBuildCount);
        eventBus.addHandler(UpdatedSystemCount.TYPE, ShellPresenter.this::onUpdatedSystemCount);
        eventBus.addHandler(SystemsCleared.TYPE, ShellPresenter.this::onSystemsCleared);
        eventBus.addHandler(BuildsCleared.TYPE, ShellPresenter.this::onBuildsCleared);
        eventBus.addHandler(UserIdChanged.TYPE, ShellPresenter.this::onUserIdChanged);
        eventBus.addHandler(NewTerminalWidget.TYPE, this::onNewTerminalWidget);
        eventBus.addHandler(TerminalClosed.TYPE, this::onTerminalClosed);
    }

    private void onNewTerminalWidget(NewTerminalWidget.Event event) {
        applicationView.addTerminalWidget(event.terminalWidget);
        applicationView.updateTerminalCount();
    }

    private void onTerminalClosed(TerminalClosed.Event event) {
        applicationView.removeTerminalWidget(event.terminalWidget);
        applicationView.updateTerminalCount();
    }

    private void onUserIdChanged(UserIdChanged.Event event) {
        // Set the cookie and reload the page (lazy!)
        setUserIdCookie(event.userId);
        Window.Location.reload();
    }

    private void onBuildsCleared(BuildsCleared.Event event) {
        onUpdatedBuildCount(new UpdatedBuildCount.Event(0));
    }

    private void onSystemsCleared(SystemsCleared.Event event) {
        onUpdatedSystemCount(new UpdatedSystemCount.Event(0));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
        // This method consolidates the cleanup of history tokens and translates them into navigation events
        Option.of(event.getValue())
                // Remove trailing slashes, they show up sporadically
                .map(string -> string.replaceAll("/*$", ""))
                // If the string is empty then go to the start page
                .map(string -> string.isEmpty() ? NameTokens.start() : string)
                // Uncomment this to get a visual indicator when the navigation token changes
                //.peek(MaterialToast::fireToast)
                .map(token -> new Navigation.Event(token, applicationView.getMainContainer()))
                .forEach(eventBus::fireEvent);
    }

    // This is a different option to inject AWSIoTData into the global namespace. This is being left here for when JSNI
    //   is deprecated by GWT in 3.0 and beyond.
    // ScriptInjector.fromString("var $wnd.AWSIoTData = require('aws-iot-device-sdk');").inject();
}
