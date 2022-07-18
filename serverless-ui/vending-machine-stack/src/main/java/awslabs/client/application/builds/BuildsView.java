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
package awslabs.client.application.builds;

import awslabs.client.application.events.BuildsCleared;
import awslabs.client.application.models.ModelList;
import awslabs.client.resources.AppResources;
import awslabs.client.shared.IotBuild;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialRow;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.util.logging.Logger;

import static awslabs.client.application.shared.GwtHelper.customTransition;

public class BuildsView implements IBuildsView {
    private static final BuildsView.Binder binder = GWT.create(BuildsView.Binder.class);

    @UiField
    MaterialRow noBuildsRow;
    @UiField
    MaterialRow buildsLoadingRow;
    @UiField
    MaterialRow buildsRow;
    @UiField
    MaterialButton reload;
    @UiField
    AppResources res;
    @Inject
    EventBus eventBus;
    @Inject
    Logger log;
    ModelList<BuildRow> buildRowList = new ModelList<>();
    private Widget root;
    private double percent;

    @Inject
    BuildsView() {
    }

    @Inject
    public void setup() {
        root = binder.createAndBindUi(this);
        res.style().ensureInjected();
    }

    public Widget getWidget() {
        return root;
    }

    @Override
    public void clear() {
        buildsRow.clear();
        buildRowList.clear();
    }

    public void showNoBuilds() {
        noBuildsRow.setVisible(true);
        buildsLoadingRow.setVisible(false);
        buildsRow.setVisible(false);
    }

    @Override
    public void showBuildsLoading() {
        noBuildsRow.setVisible(false);
        buildsLoadingRow.setVisible(true);
        buildsRow.setVisible(false);
    }

    @Override
    public void showBuilds() {
        if (buildRowList.size() == 0) {
            showNoBuilds();
            return;
        }

        noBuildsRow.setVisible(false);
        buildsLoadingRow.setVisible(false);
        buildsRow.setVisible(true);
    }

    @Override
    public Option<IotBuild> updateBuild(BuildsPresenter buildsPresenter, IotBuild iotBuild) {
        // Show the builds
        showBuilds();

        Option<BuildRow> buildUiOption = buildRowList.getModelOption(iotBuild.name());

        if (buildUiOption.isEmpty()) {
            // We do not have this model, ignore it
            return Option.none();
        }

        // Only update the build if the build is available or there is a comment
        if (!iotBuild.buildAvailable() && iotBuild.commentOption().isEmpty()) {
            // Nothing to update, just return
            return Option.none();
        }

        BuildRow buildRow = buildUiOption.get();
        buildRow.update(iotBuild);

        return Option.of(iotBuild);
    }

    @Override
    public Option<IotBuild> addBuild(BuildsPresenter buildsPresenter, IotBuild iotBuild) {
        Option<BuildRow> buildUiOption = buildRowList.getModelOption(iotBuild.name());

        if (buildUiOption.isDefined()) {
            log.info("Received a duplicate add request for an existing build, ignoring");
            return Option.none();
        }

        BuildRow buildRow = new BuildRow(buildsPresenter, iotBuild);
        buildRowList.append(buildRow);

        // Show the builds
        showBuilds();

        buildsRow.add(buildRow);

        return Option.of(iotBuild);
    }

    @Override
    public Option<IotBuild> finishBuild(BuildsPresenter buildsPresenter, IotBuild iotBuild) {
        Option<BuildRow> buildUiOption = buildRowList.getModelOption(iotBuild.name());

        if (buildUiOption.isEmpty()) {
            log.info("Received a finished message for an unknown build, ignoring");
            return Option.none();
        }

        // Show the builds
        showBuilds();

        BuildRow buildRow = buildUiOption.get();
        buildRow.finished(buildRow.getBuild());

        return Option.of(iotBuild);
    }

    @UiHandler("reload")
    public void onReloadClicked(ClickEvent clickEvent) {
        eventBus.fireEvent(new BuildsCleared.Event());

        customTransition(reload, res, res.style().refresh());
    }

    interface Binder extends UiBinder<Widget, BuildsView> {
    }
}
