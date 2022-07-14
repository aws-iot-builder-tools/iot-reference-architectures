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
package awslabs.client.application.systems;

import awslabs.client.application.events.SystemsCleared;
import awslabs.client.application.models.ModelList;
import awslabs.client.resources.AppResources;
import awslabs.client.shared.IotSystem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static awslabs.client.application.shared.GwtHelper.customTransition;

public class SystemsView implements ISystemsView {
    private static final SystemsView.Binder binder = GWT.create(SystemsView.Binder.class);
    @UiField
    MaterialRow systemsRow;
    @UiField
    MaterialRow noSystemsRow;
    @UiField
    MaterialRow systemsLoadingRow;
    @UiField
    MaterialButton reload;
    @UiField
    MaterialContainer container;
    @UiField
    AppResources res;
    @Inject
    EventBus eventBus;
    @Inject
    Logger log;
    ModelList<SystemRow> systemRowList = new ModelList<>();
    private Widget root;

    @Inject
    SystemsView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);
        res.style().ensureInjected();
    }

    @Override
    public Widget getWidget() {
        return root;
    }

    public void showNoSystems() {
        noSystemsRow.setVisible(true);
        systemsLoadingRow.setVisible(false);
        systemsRow.setVisible(false);
    }

    @Override
    public void showSystemsLoading() {
        noSystemsRow.setVisible(false);
        systemsLoadingRow.setVisible(true);
        systemsRow.setVisible(false);
    }

    @Override
    public void showSystems() {
        if (systemRowList.size() == 0) {
            showNoSystems();
            return;
        }

        noSystemsRow.setVisible(false);
        systemsLoadingRow.setVisible(false);
        systemsRow.setVisible(true);
    }

    @Override
    public Option<IotSystem> updateSystem(ISystemsPresenter systemsPresenter, IotSystem iotSystem) {
        Option<SystemRow> systemUiOption = systemRowList.getModelOption(iotSystem.name());

        if (systemUiOption.isEmpty()) {
            addSystem(systemsPresenter, iotSystem);
            return Option.of(iotSystem);
        }

        // Show the systems
        showSystems();

        SystemRow systemRow = systemUiOption.get();
        systemRow.update(iotSystem);

        return Option.of(iotSystem);
    }

    @Override
    public Option<IotSystem> addSystem(ISystemsPresenter systemsPresenter, IotSystem iotSystem) {
        Option<SystemRow> systemUiOption = systemRowList.getModelOption(iotSystem.name());

        if (systemUiOption.isDefined()) {
            log.info("Received a duplicate add request for an existing system, ignoring");

            // Show the systems
            showSystems();

            return Option.none();
        }

        SystemRow systemRow = new SystemRow(eventBus, iotSystem);
        systemRowList.append(systemRow);

        // Show the systems
        showSystems();

        systemsRow.add(systemRow);

        List<SystemRow> sortedRows = systemsRow.getChildrenList().stream()
                .filter(widget -> widget instanceof SystemRow)
                .map(widget -> (SystemRow) widget)
                .sorted(Comparator.comparing(SystemRow::name))
                .collect(Collectors.toList());

        systemsRow.clear();

        sortedRows.forEach(row -> systemsRow.add(row));

        return Option.of(iotSystem);
    }

    @Override
    public void clear() {
        systemsRow.clear();
        systemRowList.clear();
    }

    @UiHandler("reload")
    public void onReloadClicked(ClickEvent clickEvent) {
        eventBus.fireEvent(new SystemsCleared.Event());

        customTransition(reload, res, res.style().refresh());
    }

    interface Binder extends UiBinder<Widget, SystemsView> {
    }
}
