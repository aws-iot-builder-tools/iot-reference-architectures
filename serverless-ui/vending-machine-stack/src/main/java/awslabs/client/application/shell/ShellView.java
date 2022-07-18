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

import awslabs.client.application.about.IAboutPresenter;
import awslabs.client.application.builds.IBuildsPresenter;
import awslabs.client.application.events.UserIdChanged;
import awslabs.client.application.raspberrypi.IRaspberryPiPresenter;
import awslabs.client.application.shared.GwtHelper;
import awslabs.client.application.systems.ISystemsPresenter;
import awslabs.client.application.terminals.ITerminalsPresenter;
import awslabs.client.application.terminals.terminal.TerminalWidget;
import awslabs.client.place.NameTokens;
import awslabs.client.ssm.SsmHelper;
import com.google.common.io.BaseEncoding;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.addins.client.cutout.MaterialCutOut;
import gwt.material.design.client.ui.*;
import gwt.material.design.client.ui.html.ListItem;
import gwt.material.design.client.ui.html.UnorderedList;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public class ShellView implements IShellView {

    private static final Binder binder = GWT.create(Binder.class);
    @UiField
    MaterialContainer mainContainer;
    @UiField
    MaterialLink builds;
    @UiField
    MaterialLink systems;
    @UiField
    MaterialLink raspberryPi;
    @UiField
    MaterialLink devBoard;
    @UiField
    MaterialBadge buildCount;
    @UiField
    MaterialBadge systemCount;
    @UiField
    MaterialBadge terminalCount;
    @UiField
    MaterialNavBrand vendingMachine;
    @UiField
    MaterialCutOut cutout;
    @UiField
    MaterialLink cancelChangeId;
    @UiField
    MaterialLink acceptChangeId;
    // NOTE: Inject these to make sure they are bound to the event bus and set up properly
    @UiField
    MaterialTextBox userId;
    @UiField
    UnorderedList terminalMenuList;
    @Inject
    IAboutPresenter aboutPresenter;
    @Inject
    IBuildsPresenter buildsPresenter;
    @Inject
    ISystemsPresenter systemsPresenter;
    @Inject
    IRaspberryPiPresenter raspberryPiPresenter;
    @Inject
    ITerminalsPresenter terminalsPresenter;
    @Inject
    EventBus eventBus;
    private Widget root;

    @Inject
    ShellView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);

        // Get rid of the splash screen
        Document.get().getElementById("splashscreen").removeFromParent();

        // Indicate we're not connected
        vendingMachine.setText("IoT vending machine (not connected)");

        Option<String> nameOption = Option.of(Window.Location.getParameter("name"));
        Option<String> idOption = Option.of(Window.Location.getParameter("id"));

        if (nameOption.isDefined() && idOption.isDefined()) {
            String name = nameOption.map(value -> BaseEncoding.base64().decode(value)).map(String::new).get();
            String id = idOption.map(value -> BaseEncoding.base64().decode(value)).map(String::new).get();
            MaterialToast.fireToast("Connecting to [" + name + "]");
            SsmHelper.connect(eventBus, name, id);
        }
    }

    @UiHandler("vendingMachine")
    public void onHeaderClicked(ClickEvent clickEvent) {
        userId.setText(ShellPresenter.userIdOption.getOrElse(""));
        cutout.setTarget(vendingMachine);
        cutout.open();
    }

    @UiHandler("acceptChangeId")
    public void onAcceptChangeIdClicked(ClickEvent clickEvent) {
        // Tell the system the user ID changed
        eventBus.fireEvent(new UserIdChanged.Event(userId.getText()));
        cutout.close();
    }

    @UiHandler("cancelChangeId")
    public void onCancelChangeIdClicked(ClickEvent clickEvent) {
        cutout.close();
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
    public void addTerminalWidget(TerminalWidget terminalWidget) {
        MaterialLink materialLink = new MaterialLink(terminalWidget.getSessionName());
        materialLink.setHref(String.join("", "#", terminalWidget.getToken()));

        terminalMenuList.add(materialLink);
    }

    @Override
    public MaterialLink getDevBoardWidget() {
        return devBoard;
    }

    @Override
    public MaterialNavBrand getNavBrand() {
        return vendingMachine;
    }

    @Override
    public void updateBuildCount(int count) {
        String text = getStringForCount(count, "build");

        buildCount.setText(text);
    }

    @Override
    public void updateSystemCount(int count) {
        String text = getStringForCount(count, "system");

        systemCount.setText(text);
    }

    @Override
    public void updateTerminalCount() {
        int count = terminalMenuList.getChildren().size();
        String text = getStringForCount(count, "terminal");

        terminalCount.setText(text);
    }

    @Override
    public void removeTerminalWidget(TerminalWidget terminalWidget) {
        // Get the list of children from the terminal menu list that are ListItems
        GwtHelper.castListElements(terminalMenuList.getChildren(), ListItem.class)
                // Get the list of children of each list item that are MaterialLinks
                .map(listItem -> Tuple.of(listItem, GwtHelper.castListElements(listItem.getChildren(), MaterialLink.class)))
                // Find any MaterialLinks that contain the name of the terminal session that is to be removed
                .map(tuple -> tuple.map2(list -> list.filter(materialLink -> materialLink.getHref().contains(terminalWidget.getSessionName()))))
                // Grab just the ListItem
                .map(tuple -> tuple._1)
                // Remove the ListItem from the terminal menu list
                .forEach(listItem -> terminalMenuList.remove(listItem));

        // Close the websocket for the terminal
        terminalWidget.close();

        History.newItem(NameTokens.terminals());
    }

    @NotNull
    private String getStringForCount(int count, String type) {
        String text = String.join(" ", String.valueOf(count), type);

        if (count != 1) {
            text = String.join("", text, "s");
        }

        return text;
    }

    interface Binder extends UiBinder<Widget, ShellView> {
    }
}
