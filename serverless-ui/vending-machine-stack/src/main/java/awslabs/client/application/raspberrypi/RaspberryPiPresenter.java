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

import awslabs.client.IotService;
import awslabs.client.IotServiceAsync;
import awslabs.client.application.events.BuildRequestSuccessful;
import awslabs.client.application.events.BuildRequestedByUser;
import awslabs.client.place.NameTokens;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialLoader;

import javax.inject.Inject;
import java.util.logging.Logger;

public class RaspberryPiPresenter implements IRaspberryPiPresenter {
    public static final IotServiceAsync IOT_SERVICE_ASYNC = GWT.create(IotService.class);

    @Inject
    EventBus eventBus;
    @Inject
    IRaspberryPiView raspberryPiView;
    @Inject
    Logger log;

    @Inject
    RaspberryPiPresenter() {
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(BuildRequestedByUser.TYPE, this::onBuildRequestedByUser);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Widget getWidget() {
        return raspberryPiView.getWidget();
    }

    @Override
    public String getToken() {
        return NameTokens.raspberryPi();
    }

    @Override
    public void onBuildRequestedByUser(BuildRequestedByUser.Event buildRequestedByUser) {
        MaterialLoader.loading(true, "Requesting your build...");

        IOT_SERVICE_ASYNC.buildImage(buildRequestedByUser.raspberryPiRequest, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                MaterialLoader.loading(false);
                String message = caught.getMessage();
                if (message.contains("504")) {
                    Window.alert("The build request took too long to respond. Please wait about 10 seconds to see if the build shows up. If it does not try the request again.");
                } else {
                    Window.alert("Failure: " + caught.getMessage());
                }
            }

            @Override
            public void onSuccess(String buildId) {
                MaterialLoader.loading(false);
                log.info("Success: " + buildId);
                eventBus.fireEvent(new BuildRequestSuccessful.Event(buildId));
            }
        });
    }
}
