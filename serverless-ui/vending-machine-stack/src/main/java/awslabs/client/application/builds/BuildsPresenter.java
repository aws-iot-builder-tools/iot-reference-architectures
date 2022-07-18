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

import awslabs.client.IotService;
import awslabs.client.IotServiceAsync;
import awslabs.client.application.shell.ShellPresenter;
import awslabs.client.application.events.*;
import awslabs.client.application.models.BuildList;
import awslabs.client.place.NameTokens;
import awslabs.client.shared.IotBuild;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialLoader;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.collection.List;

import javax.inject.Inject;
import java.util.logging.Logger;

public class BuildsPresenter implements IBuildsPresenter {
    public static final IotServiceAsync IOT_SERVICE_ASYNC = GWT.create(IotService.class);
    public static final int DEFAULT_DELAY_MILLIS = 5000;

    @Inject
    BuildList buildList;

    @Inject
    EventBus eventBus;

    @Inject
    IBuildsView buildsView;

    @Inject
    Logger log;

    @Inject
    BuildsPresenter() {
    }

    @Inject
    public void setup() {
        setupTimer();
    }

    private void setupTimer() {
        Timer timer = new Timer() {
            @Override
            public void run() {
                if (ShellPresenter.userIdOption.isEmpty()) {
                    schedule(DEFAULT_DELAY_MILLIS);
                    return;
                }

                IOT_SERVICE_ASYNC.getBuildList(ShellPresenter.userIdOption.get(), new AsyncCallback<java.util.List<IotBuild>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        log.info("Failed to get the system list, retrying");
                        schedule(DEFAULT_DELAY_MILLIS);
                    }

                    @Override
                    public void onSuccess(java.util.List<IotBuild> newBuildList) {
                        List.ofAll(newBuildList)
                                .flatMap(build -> buildList.addModel(build))
                                .map(build -> buildsView.addBuild(BuildsPresenter.this, build));

                        eventBus.fireEvent(new UpdatedBuildCount.Event(buildList.size()));

                        schedule(DEFAULT_DELAY_MILLIS);
                    }
                });
            }
        };

        // Schedule the timer to run once in 5 seconds
        timer.schedule(DEFAULT_DELAY_MILLIS);
    }

    private void onBuildStarted(BuildStarted.Event buildStartedEvent) {
        MaterialToast.fireToast("Build [" + buildStartedEvent.buildId + "] started!");

        addBuild(buildStartedEvent.buildId);
    }

    private void addBuild(String buildId) {
        IotBuild iotBuild = new IotBuild(buildId);

        buildList.addModel(iotBuild)
                // If a new build was added, update the view
                .map(value -> buildsView.addBuild(this, value));

        eventBus.fireEvent(new UpdatedBuildCount.Event(buildList.size()));
    }

    private void onBuildFinished(BuildFinished.Event buildFinishedEvent) {
        MaterialToast.fireToast("Build [" + buildFinishedEvent.buildId + "] finished!");

        buildList.buildFinished(buildFinishedEvent.buildId)
                // If there was an update then update the view
                .map(build -> buildsView.finishBuild(this, build));
    }

    private void onBuildProgress(BuildProgress.Event buildProgressEvent) {
        buildList.buildProgress(buildProgressEvent)
                // If there was an update then update the view
                .map(build -> buildsView.updateBuild(this, build));
    }

    private void onBuildsCleared(BuildsCleared.Event event) {
        buildsView.clear();
        buildList.clear();
        buildsView.showBuildsLoading();
    }

    @Override
    public void downloadImage(String buildId) {
        MaterialLoader.loading(true);
        eventBus.fireEvent(new ShowMaterialLoader.Event(true));

        IOT_SERVICE_ASYNC.getPresignedS3Url(buildId, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                eventBus.fireEvent(new ShowMaterialLoader.Event(false));
                MaterialLoader.loading(false);
                MaterialToast.fireToast("Failed to get the presigned URL for the image");
            }

            @Override
            public void onSuccess(String result) {
                eventBus.fireEvent(new ShowMaterialLoader.Event(false));
                MaterialLoader.loading(false);
                Window.open(result, "_self", "enabled");
            }
        });
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(BuildStarted.TYPE, this::onBuildStarted);
        eventBus.addHandler(BuildProgress.TYPE, this::onBuildProgress);
        eventBus.addHandler(BuildFinished.TYPE, this::onBuildFinished);
        eventBus.addHandler(BuildsCleared.TYPE, this::onBuildsCleared);
        eventBus.addHandler(UpdatedBuildCount.TYPE, this::onUpdatedBuildCount);
    }

    private void onUpdatedBuildCount(UpdatedBuildCount.Event event) {
        buildsView.showBuilds();
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Widget getWidget() {
        return buildsView.getWidget();
    }

    @Override
    public String getToken() {
        return NameTokens.builds();
    }
}
