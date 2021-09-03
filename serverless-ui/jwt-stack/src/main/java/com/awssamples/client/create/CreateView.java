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
package com.awssamples.client.create;

import com.awssamples.client.PrettyPre;
import com.awssamples.client.events.DeviceIdChanged;
import com.awssamples.client.events.RequestJwt;
import com.awssamples.client.events.ValidateJwt;
import com.awssamples.client.shared.JwtCreationResponse;
import com.awssamples.client.shared.JwtService;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.base.error.BasicEditorError;
import gwt.material.design.client.base.validator.ValidationChangedEvent;
import gwt.material.design.client.base.validator.Validator;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialTextBox;
import io.vavr.control.Try;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CreateView implements ICreateView {
    private static final Binder binder = GWT.create(Binder.class);

    interface Binder extends UiBinder<Widget, CreateView> {
    }

    @UiField
    MaterialTextBox deviceId;
    @UiField
    MaterialTextBox iccid;
    @UiField
    MaterialTextBox expirationInSeconds;
    @UiField
    MaterialPanel jwtData;
    @UiField
    MaterialLink generateJwtButton;
    @UiField
    MaterialLink validateJwtButton;
    @Inject
    EventBus eventBus;
    @Inject
    Logger log;
    private Widget root;

    @Inject
    CreateView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);

        expirationInSeconds.addValidator(new Validator<String>() {
            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> errors = new ArrayList<>();

                Try<Integer> parseIntTry = Try.of(() -> Integer.parseInt(value));

                if (parseIntTry.isFailure()) {
                    // Failed to parse as an integer, return early since none of the other errors apply
                    errors.add(new BasicEditorError(editor, value, "Not a valid integer"));

                    return errors;
                }

                int intValue = parseIntTry.get();

                if (intValue < JwtService.EXPIRATION_IN_SECONDS_MIN) {
                    // Too low
                    errors.add(new BasicEditorError(editor, value, "Value is below the minimum [" + JwtService.EXPIRATION_IN_SECONDS_MIN + "]"));
                } else if (intValue > JwtService.EXPIRATION_IN_SECONDS_MAX) {
                    // Too high
                    errors.add(new BasicEditorError(editor, value, "Value is above the maximum [" + JwtService.EXPIRATION_IN_SECONDS_MAX + "]"));
                }

                return errors;

                // Check to see if the value is valid
                /* Can't use this in GWT!
                Match(parseIntTry)
                        .option(
                                // Failed to parse as an integer
                                Case($Failure($()), n -> new BasicEditorError(editor, value, "Not a valid integer")),
                                // Too low
                                Case($Success($(n -> n < EXPIRATION_IN_SECONDS_MIN)), n -> new BasicEditorError(editor, value, "Value is below the minimum [" + EXPIRATION_IN_SECONDS_MIN + "]")),
                                // Too high
                                Case($Success($(n -> n > EXPIRATION_IN_SECONDS_MAX)), n -> new BasicEditorError(editor, value, "Value is above the maximum [" + EXPIRATION_IN_SECONDS_MAX + "]"))
                        )
                        // If any error was generated then add them to the error list
                        .forEach(errors::add);
                 */
            }
        });

        expirationInSeconds.validate(true);
        enableDisableButtons();
    }

    public Widget getWidget() {
        return root;
    }

    @UiHandler("deviceId")
    public void onDeviceIdChanged(ValueChangeEvent<String> valueChangeEvent) {
        sendDeviceIdUpdate();
    }

    @UiHandler("deviceId")
    public void onDeviceIdKeyUp(KeyUpEvent keyUpEvent) {
        sendDeviceIdUpdate();
    }

    @UiHandler("expirationInSeconds")
    public void onExpirationInSecondsKeyUp(KeyUpEvent keyUpEvent) {
        expirationInSeconds.validate(true);
    }

    @UiHandler("expirationInSeconds")
    public void onExpirationInSecondsChange(ChangeEvent changeEvent) {
        expirationInSeconds.validate(true);
    }

    @UiHandler("expirationInSeconds")
    public void onExpirationInSecondsChange(ValidationChangedEvent validationChangedEvent) {
        enableDisableButtons();
    }

    private void enableDisableButtons() {
        enableDisableGenerateButton();
        enableDisableValidateButton();
    }

    private void enableDisableGenerateButton() {
        generateJwtButton.setEnabled(expirationInSeconds.validate());
    }

    private void enableDisableValidateButton() {
        boolean hasJwtData = jwtData.getChildrenList().stream().anyMatch(widget -> widget instanceof PrettyPre);
        validateJwtButton.setEnabled(hasJwtData);
    }

    @UiHandler("generateJwtButton")
    public void onGenerateJwtButtonClicked(ClickEvent clickEvent) {
        eventBus.fireEvent(new RequestJwt.Event(iccid.getValue(), Integer.parseInt(expirationInSeconds.getValue())));
    }

    @UiHandler("validateJwtButton")
    public void onValidateJwtButtonClicked(ClickEvent clickEvent) {
        eventBus.fireEvent(new ValidateJwt.Event());
    }

    private void sendDeviceIdUpdate() {
        eventBus.fireEvent(new DeviceIdChanged.Event(deviceId.getValue()));
    }

    @Override
    public void updateIccid(String iccid) {
        this.iccid.setText(iccid);
    }

    @Override
    public void updateJwtData(JwtCreationResponse jwtCreationResponse) {
        PrettyPre prettyPre = new PrettyPre(jwtCreationResponse.decodedJwt);
        jwtData.clear();
        jwtData.add(prettyPre);

        enableDisableButtons();
    }
}
