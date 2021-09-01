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
package com.awssamples.client.attribution;

import com.awssamples.client.events.AttributionChanged;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.base.error.BasicEditorError;
import gwt.material.design.client.base.validator.Validator;
import gwt.material.design.client.ui.MaterialCheckBox;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialTextBox;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AttributionView implements IAttributionView {
    private static final Binder binder = GWT.create(Binder.class);

    interface Binder extends UiBinder<Widget, AttributionView> {
    }

    @UiField
    MaterialCheckBox attributionCheckBox;
    @UiField
    MaterialTextBox partnerName;
    @UiField
    MaterialTextBox solutionName;
    @UiField
    MaterialTextBox versionName;
    @UiField
    MaterialLabel attributionLabel;
    @Inject
    EventBus eventBus;
    @Inject
    Logger log;
    private Widget root;

    //    private static final Pattern apnRx = Pattern.compile("^APN\\/1\\s((\\w){1,64}),(\\w{1,64})(,(([\\w\\.]){1,8}))?$");
    private static final String whiteSpaceCharacterClass = "[ \\t\\n\\x0B\\f\\r]";
    private static final String wordCharacterClass = "[a-zA-Z_0-9]";
    private static final String invertedWordCharacterClass = "[^a-zA-Z_0-9]";
    private static final String wordCharacterClassWithDot = "[a-zA-Z_0-9.]";
    private static final String invertedWordCharacterClassWithDot = "[^a-zA-Z_0-9.]";
    private static final String temp = "^APN\\/1" + whiteSpaceCharacterClass + "((" + wordCharacterClass + "){1,64}),(" + wordCharacterClass + "{1,64})(,((" + wordCharacterClassWithDot + "){1,8}))?$";

    @Inject
    AttributionView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);

        partnerName.addValidator(getValidator(wordCharacterClass, invertedWordCharacterClass));
        solutionName.addValidator(getValidator(wordCharacterClass, invertedWordCharacterClass));
        versionName.addValidator(getValidator(wordCharacterClassWithDot, invertedWordCharacterClassWithDot));

        enableDisableFields();
    }

    @NotNull
    private Validator<String> getValidator(String validPattern, String invalidPattern) {
        return new Validator<String>() {
            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> errors = new ArrayList<>();

                if (value.matches(invalidPattern)) {
                    MaterialToast.fireToast("value matched: " + value);
                    errors.add(new BasicEditorError(editor, value, "Contains an invalid character (not in " + validPattern + ")"));
                } else {
                    MaterialToast.fireToast("value didn't match: " + value);
                }

                return errors;
            }
        };
    }

    @UiHandler("partnerName")
    public void onPartnerNameChanged(ValueChangeEvent<String> valueChangeEvent) {
        partnerName.validate(true);
    }

    @UiHandler("partnerName")
    public void onPartnerNameChanged(KeyUpEvent keyUpEvent) {
        partnerName.validate(true);
    }

    @UiHandler("solutionName")
    public void onSolutionNameChanged(ValueChangeEvent<String> valueChangeEvent) {
        solutionName.validate(true);
    }

    @UiHandler("solutionName")
    public void onSolutionNameChanged(KeyUpEvent keyUpEvent) {
        solutionName.validate(true);
    }

    @UiHandler("versionName")
    public void onVersionNameChanged(ValueChangeEvent<String> valueChangeEvent) {
        versionName.validate(true);
    }

    @UiHandler("versionName")
    public void onVersionNameChanged(KeyUpEvent keyUpEvent) {
        versionName.validate(true);
    }

    private void enableDisableFields() {
        boolean attributionEnabled = isAttributionEnabled();

        partnerName.setEnabled(attributionEnabled);
        solutionName.setEnabled(attributionEnabled);
        versionName.setEnabled(attributionEnabled);
        attributionLabel.setVisible(attributionEnabled);

        attributionLabel.setText("Attribution string: " + buildAttributionString());
    }

    private boolean isAttributionEnabled() {
        return attributionCheckBox.getValue();
    }

    private String buildAttributionString() {
        String attributionString = String.join("", "APN/1 ", partnerName.getValue());

        if (solutionName.getValue().isEmpty()) {
            return attributionString;
        }

        attributionString = String.join(",", attributionString, solutionName.getValue());

        if (versionName.getValue().isEmpty()) {
            return attributionString;
        }

        return String.join(",", attributionString, versionName.getValue());
    }

    @UiHandler("attributionCheckBox")
    public void onAttributionCheckBoxChanged(ValueChangeEvent<Boolean> event) {
        enableDisableFields();

        if (isAttributionEnabled()) {
            eventBus.fireEvent(new AttributionChanged.Event(Option.of(buildAttributionString())));
        } else {
            eventBus.fireEvent(new AttributionChanged.Event(Option.none()));
        }
    }

    public Widget getWidget() {
        return root;
    }
}
