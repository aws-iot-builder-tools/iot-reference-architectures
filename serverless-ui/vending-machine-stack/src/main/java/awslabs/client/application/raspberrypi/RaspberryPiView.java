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
package awslabs.client.application.raspberrypi;

import awslabs.client.application.events.BuildRequestedByUser;
import awslabs.client.shared.RaspberryPiRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.constants.InputType;
import gwt.material.design.client.ui.MaterialCheckBox;
import gwt.material.design.client.ui.MaterialTextBox;
import io.vavr.control.Try;

import javax.inject.Inject;
import java.util.Optional;

import static awslabs.client.application.shell.ShellPresenter.userIdOption;

public class RaspberryPiView implements IRaspberryPiView {
    private static final RaspberryPiView.Binder binder = GWT.create(RaspberryPiView.Binder.class);
    @UiField
    MaterialTextBox imageName;
    @UiField
    MaterialCheckBox wifiEnabled;
    @UiField
    MaterialCheckBox wifiPasswordHidden;
    @UiField
    MaterialTextBox wifiSsid;
    @UiField
    MaterialTextBox wifiPassword;
    @UiField
    MaterialCheckBox oneWireEnabled;
    @UiField
    MaterialTextBox oneWirePin;
    @UiField
    MaterialCheckBox ssmEnabled;
    @UiField
    MaterialCheckBox addPiAccount;
    @Inject
    EventBus eventBus;
    private Widget root;
    @Inject
    RaspberryPiView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);

        oneWirePin.addValueChangeHandler(event -> {
            // Attempt to parse the string into an integer
            Try<Integer> integerTry = Try.of(() -> Integer.parseInt(event.getValue()));

            if (integerTry.isSuccess() && integerTry.get() > 0) {
                // Nothing to do, the data was valid
                return;
            }

            integerTry
                    // Make sure it cannot go below zero
                    .map(value -> Math.max(0, value))
                    // If an error occurs we use the default pin 4 as the fallback
                    .recover(exception -> 4)
                    // Convert it back to a string
                    .map(String::valueOf)
                    // Set the value
                    .forEach(value -> oneWirePin.setValue(value));
        });
    }

    @Override
    public Widget getWidget() {
        return root;
    }

    @UiHandler("wifiEnabled")
    void onWifiEnabledClicked(ClickEvent event) {
        boolean visible = wifiEnabled.getValue();

        wifiSsid.setVisible(visible);
        wifiPassword.setVisible(visible);
        wifiPasswordHidden.setVisible(visible);
    }

    @UiHandler("wifiPasswordHidden")
    void onWifiPasswordHidden(ClickEvent event) {
        if (wifiPasswordHidden.getValue()) {
            wifiPassword.setType(InputType.PASSWORD);
        } else {
            wifiPassword.setType(InputType.TEXT);
        }
    }

    @UiHandler("oneWireEnabled")
    void onOneWireEnabledClicked(ClickEvent event) {
        oneWirePin.setVisible(oneWireEnabled.getValue());
    }

    @UiHandler("build")
    void onBuildClicked(ClickEvent event) {
        RaspberryPiRequest raspberryPiRequest = new RaspberryPiRequest();
        raspberryPiRequest.settings.imageName = Optional.of(imageName.getValue()).orElse("unnamed");
        raspberryPiRequest.settings.userId = userIdOption.get();
        raspberryPiRequest.settings.ssmEnabled = ssmEnabled.getValue();
        raspberryPiRequest.settings.addPiAccount = addPiAccount.getValue();
        raspberryPiRequest.settings.wifiSsidNullable = wifiEnabled.getValue() ? wifiSsid.getValue() : null;
        raspberryPiRequest.settings.wifiPasswordNullable = wifiEnabled.getValue() ? wifiPassword.getValue() : null;
        raspberryPiRequest.settings.oneWireEnabled = oneWireEnabled.getValue();
        raspberryPiRequest.settings.oneWirePinNullable = oneWireEnabled.getValue() ? Integer.parseInt(oneWirePin.getValue()) : null;

        eventBus.fireEvent(new BuildRequestedByUser.Event(raspberryPiRequest));
    }

    interface Binder extends UiBinder<Widget, RaspberryPiView> {
    }
}
