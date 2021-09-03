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
package com.awssamples.client.shell;

import com.awssamples.client.DaggerAppInjector;
import com.awssamples.client.ReceivesEvents;
import com.awssamples.client.attribution.IAttributionPresenter;
import com.awssamples.client.create.ICreatePresenter;
import com.awssamples.client.events.*;
import com.awssamples.client.mqtt.AWSIoTData;
import com.awssamples.client.mqtt.ClientConfig;
import com.awssamples.client.mqtt.MqttClient;
import com.awssamples.client.place.NameTokens;
import com.awssamples.client.shared.JwtService;
import com.awssamples.client.shared.JwtServiceAsync;
import com.awssamples.client.test.ITestPresenter;
import com.awssamples.client.GwtHelpers;
import com.awssamples.client.events.*;
import com.google.gwt.core.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import gwt.material.design.client.constants.Color;
import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.inject.Inject;

public class ShellPresenter implements EntryPoint, ReceivesEvents, ValueChangeHandler<String> {
    public static final JwtServiceAsync JWT_SERVICE_ASYNC = GWT.create(JwtService.class);
    public static final String WEBSOCKET_PROTOCOL = "wss";
    private static final String TOPIC_PREFIX = String.join("/", "clients", "jwt");
    public static final String USER_ID_COOKIE_NAME = "userId";
    public static Option<String> clientIdOption = Option.none();
    public static Option<String> userIdOption = Option.none();
    @Inject
    EventBus eventBus;
    @Inject
    IShellView shellView;
    @Inject
    ICreatePresenter createPresenter;
    @Inject
    IAttributionPresenter attributionPresenter;
    @Inject
    ITestPresenter testPresenter;
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

        JWT_SERVICE_ASYNC.getClientConfig(
                new AsyncCallback<ClientConfig>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        eventBus.fireEvent(new MqttConnectionFailed.Event());
                        Window.alert("Couldn't get credentials for an MQTT connection [" + caught.getMessage() + "]");
                    }

                    @Override
                    public void onSuccess(ClientConfig clientConfig) {
                        eventBus.fireEvent(new RegionDetected.Event(clientConfig.region));
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

                        mqttClient.subscribe(String.join("/", TOPIC_PREFIX, "+"));

                        eventBus.fireEvent(new MqttConnectionSuccessful.Event(mqttClient));
                    }
                });
    }

    public void onConnectionAttempt(MqttConnectionAttempt.Event mqttConnectionAttemptEvent) {
        shellView.getDevBoardWidget().setIconColor(Color.CYAN);
    }

    public void onConnectionFailed(MqttConnectionFailed.Event mqttConnectionFailedEvent) {
        shellView.getDevBoardWidget().setIconColor(Color.RED);
    }

    public void onConnectionSuccessful(MqttConnectionSuccessful.Event mqttConnectionSuccessfulEvent) {
        shellView.getDevBoardWidget().setIconColor(Color.GREEN);
        mqttConnectionSuccessfulEvent.mqttClient.onMessageCallback(ShellPresenter.this::onMessage);
    }

    public void onMessage(String topic, Object payload) {
        Option<String> payloadStringOption = Option.of(payload)
                .map(Object::toString);

        if (payloadStringOption.isEmpty()) {
            Window.alert("NULL payload on topic: " + topic + ", this is a bug");
            return;
        }

        String payloadString = payloadStringOption.get();

        eventBus.fireEvent(new MqttMessage.Event(topic, payloadString));
    }

    @Override
    public void onModuleLoad() {
        // Must use schedule deferred to make sure everything is loaded before we try to do anything
        // https://gitter.im/GwtMaterialDesign/gwt-material?at=58868073c0de6f017fe11c7d (Marcin Szałomski @baldram Jan 23 2017 17:15)
        Scheduler.get().scheduleDeferred(() -> {
            // Inject the dependencies
            Try.run(() -> DaggerAppInjector.create().inject(this))
                    // Set the application view as the root
                    .andThen(() -> RootPanel.get().add(shellView.getWidget()))
                    // All injected event receivers are already bound at this point except this class because it can't be injected
                    //   into itself. So we bind this final class here.
                    .andThen(this::bindEventBus)
                    .onFailure(this::logThrowable);
        });
    }

    private void logThrowable(Throwable throwable) {
        GwtHelpers.info("Exception!");
        GwtHelpers.info("Exception! " + throwable.getMessage());
        throwable.printStackTrace();
        throw new RuntimeException(throwable);
    }


    @Override
    public void bindEventBus() {
        eventBus.addHandler(MqttConnectionAttempt.TYPE, ShellPresenter.this::onConnectionAttempt);
        eventBus.addHandler(MqttConnectionFailed.TYPE, ShellPresenter.this::onConnectionFailed);
        eventBus.addHandler(MqttConnectionSuccessful.TYPE, ShellPresenter.this::onConnectionSuccessful);
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
                .map(token -> new Navigation.Event(token, shellView.getMainContainer()))
                .forEach(eventBus::fireEvent);
    }

    // This is a different option to inject AWSIoTData into the global namespace. This is being left here for when JSNI
    //   is deprecated by GWT in 3.0 and beyond.
    // ScriptInjector.fromString("var $wnd.AWSIoTData = require('aws-iot-device-sdk');").inject();
}
