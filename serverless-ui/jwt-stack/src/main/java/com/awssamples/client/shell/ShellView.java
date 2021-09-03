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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialLink;

import javax.inject.Inject;

public class ShellView implements IShellView {
    interface Binder extends UiBinder<Widget, ShellView> {
    }

    private static final Binder binder = GWT.create(Binder.class);
    @UiField
    MaterialContainer mainContainer;
    @UiField
    MaterialLink devBoard;

    private Widget root;

    @Inject
    ShellView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);

        // Get rid of the splash screen
        Document.get().getElementById("splashscreen").removeFromParent();
    }

    @Override
    public Widget getWidget() {
        return root;
    }

    @Override
    public MaterialContainer getMainContainer() {
        return mainContainer;
    }

    @Override
    public MaterialLink getDevBoardWidget() {
        return devBoard;
    }
}
