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
package com.awssamples.client.test;

import com.awssamples.client.GwtHelpers;
import com.awssamples.client.PrettyPre;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.*;
import io.vavr.collection.List;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.util.Date;
import java.util.logging.Logger;

public class TestView implements ITestView {
    private static final Binder binder = GWT.create(Binder.class);
    private Option<String> testInvokeAuthorizerMqttCommandOption = Option.none();
    private Option<String> testInvokeAuthorizerHttpCommandOption = Option.none();
    private Option<String> testInvokeAuthorizerSignatureCommandOption = Option.none();
    private Option<String> mosquittoPublishCommandOption = Option.none();
    private Option<String> curlPublishCommandOption = Option.none();
    private static final int MAX_MESSAGES = 5;

    interface Binder extends UiBinder<Widget, TestView> {
    }

    @UiField
    MaterialCollection messageCollection;
    @UiField
    MaterialPanel testInvokeAuthorizerMqtt;
    @UiField
    MaterialPanel testInvokeAuthorizerHttp;
    @UiField
    MaterialPanel testInvokeAuthorizerWithSignature;
    @UiField
    MaterialPanel mosquittoPublish;
    @UiField
    MaterialPanel curlPublish;
    @UiField
    MaterialIcon invokeMqttCopy;
    @UiField
    MaterialIcon invokeHttpCopy;
    @UiField
    MaterialIcon invokeSignatureCopy;
    @UiField
    MaterialIcon mosquittoPublishCopy;
    @UiField
    MaterialIcon curlPublishCopy;
    @Inject
    EventBus eventBus;
    @Inject
    Logger log;
    private Widget root;

    @Inject
    TestView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);
    }

    public Widget getWidget() {
        return root;
    }

    public void updateTestInvokeAuthorizerMqtt(String command) {
        testInvokeAuthorizerMqttCommandOption = Option.of(command);
        prettyPrintAndUpdate(command, testInvokeAuthorizerMqtt);
    }

    @Override
    public void updateTestInvokeAuthorizerHttp(String command) {
        testInvokeAuthorizerHttpCommandOption = Option.of(command);
        prettyPrintAndUpdate(command, testInvokeAuthorizerHttp);
    }

    @Override
    public void updateTestInvokeAuthorizerWithSignature(String command) {
        testInvokeAuthorizerSignatureCommandOption = Option.of(command);
        prettyPrintAndUpdate(command, testInvokeAuthorizerWithSignature);
    }

    @Override
    public void updateMosquittoPubCommand(String command) {
        mosquittoPublishCommandOption = Option.of(command);
        prettyPrintAndUpdate(command, mosquittoPublish);
    }

    @Override
    public void updateCurlPubCommand(String command) {
        curlPublishCommandOption = Option.of(command);
        prettyPrintAndUpdate(command, curlPublish);
    }

    @Override
    public void addMqttMessage(String topic, String payload) {
        List<Widget> tempMessages = List.ofAll(messageCollection.getChildrenList())
                .take(MAX_MESSAGES - 1);

        messageCollection.clear();

        tempMessages
                .forEach(messageCollection::add);

        MaterialCollectionItem newMessage = new MaterialCollectionItem();
        MaterialIcon materialIcon = new MaterialIcon(IconType.MESSAGE);
        MaterialLabel topicLabel = new MaterialLabel("Topic: " + topic);
        MaterialLabel payloadLabel = new MaterialLabel("Payload: " + payload);
        MaterialLabel timestampLabel = new MaterialLabel(new Date().toString());

        newMessage.add(materialIcon);
        newMessage.add(topicLabel);
        newMessage.add(payloadLabel);
        newMessage.add(timestampLabel);

        messageCollection.insert(newMessage, 0);
    }

    private void prettyPrintAndUpdate(String command, MaterialPanel curlPublish) {
        PrettyPre prettyPre = new PrettyPre(command);
        curlPublish.clear();
        curlPublish.add(prettyPre);
    }

    @UiHandler("invokeMqttCopy")
    public void onMqttCopyClick(ClickEvent clickEvent) {
        testInvokeAuthorizerMqttCommandOption.forEach(GwtHelpers::copyThis);
        clickEvent.stopPropagation();
    }

    @UiHandler("invokeHttpCopy")
    public void onHttpCopyClick(ClickEvent clickEvent) {
        testInvokeAuthorizerHttpCommandOption.forEach(GwtHelpers::copyThis);
        clickEvent.stopPropagation();
    }

    @UiHandler("invokeSignatureCopy")
    public void onSignatureCopyClick(ClickEvent clickEvent) {
        testInvokeAuthorizerSignatureCommandOption.forEach(GwtHelpers::copyThis);
        clickEvent.stopPropagation();
    }

    @UiHandler("mosquittoPublishCopy")
    public void onMosquittoPublishCopyClick(ClickEvent clickEvent) {
        mosquittoPublishCommandOption.forEach(GwtHelpers::copyThis);
        clickEvent.stopPropagation();
    }

    @UiHandler("curlPublishCopy")
    public void onCurlPublishCopyClick(ClickEvent clickEvent) {
        curlPublishCommandOption.forEach(GwtHelpers::copyThis);
        clickEvent.stopPropagation();
    }
}
