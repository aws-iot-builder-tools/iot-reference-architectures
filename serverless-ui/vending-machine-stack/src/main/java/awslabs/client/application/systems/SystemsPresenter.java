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

import awslabs.client.IotService;
import awslabs.client.IotServiceAsync;
import awslabs.client.application.shell.ShellPresenter;
import awslabs.client.application.events.*;
import awslabs.client.application.models.SystemList;
import awslabs.client.place.NameTokens;
import awslabs.client.shared.IotSystem;
import awslabs.client.ssm.SsmConfig;
import awslabs.client.ssm.SsmWebSocket;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialLoader;
import io.vavr.collection.List;

import javax.inject.Inject;

import static awslabs.client.application.shared.GwtHelper.info;

public class SystemsPresenter implements ISystemsPresenter {
    public static final IotServiceAsync IOT_SERVICE_ASYNC = GWT.create(IotService.class);
    public static final int DEFAULT_DELAY_MILLIS = 5000;
    private static final SystemList systemList = new SystemList();
    @Inject
    EventBus eventBus;
    @Inject
    ISystemsView systemsView;

    @Inject
    SystemsPresenter() {
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

                IOT_SERVICE_ASYNC.getSystemList(ShellPresenter.userIdOption.get(), new AsyncCallback<java.util.List<IotSystem>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        info("Failed to get the system list, retrying");

                        schedule(DEFAULT_DELAY_MILLIS);
                    }

                    @Override
                    public void onSuccess(java.util.List<IotSystem> systemList) {
                        List.ofAll(systemList)
                                .forEach(system -> systemsView.updateSystem(SystemsPresenter.this, system));

                        eventBus.fireEvent(new UpdatedSystemCount.Event(systemList.size()));

                        schedule(DEFAULT_DELAY_MILLIS);
                    }
                });
            }
        };

        // Schedule the timer to run once in 5 seconds
        timer.schedule(DEFAULT_DELAY_MILLIS);
    }

    public void onSystemCreated(SystemCreated.Event systemCreatedEvent) {
        info("A new SSM activation has been created");
    }

    public void onSystemsCleared(SystemsCleared.Event event) {
        systemsView.clear();
        systemList.clear();
        systemsView.showSystemsLoading();
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(SystemCreated.TYPE, this::onSystemCreated);
        eventBus.addHandler(SystemsCleared.TYPE, this::onSystemsCleared);
        eventBus.addHandler(UpdatedSystemCount.TYPE, this::onUpdatedSystemCount);
    }

    private void onUpdatedSystemCount(UpdatedSystemCount.Event event) {
        systemsView.showSystems();
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Widget getWidget() {
        return systemsView.getWidget();
    }

    @Override
    public String getToken() {
        return NameTokens.systems();
    }
}
