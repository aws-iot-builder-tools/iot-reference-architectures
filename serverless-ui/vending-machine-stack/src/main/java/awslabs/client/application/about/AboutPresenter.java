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
package awslabs.client.application.about;

import awslabs.client.place.NameTokens;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Widget;

import javax.inject.Inject;

public class AboutPresenter implements IAboutPresenter {
    @Inject
    EventBus eventBus;
    @Inject
    IAboutView aboutView;

    @Inject
    AboutPresenter() {
    }

    @Override
    public void bindEventBus() {
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Widget getWidget() {
        return aboutView.getWidget();
    }

    @Override
    public String getToken() {
        return NameTokens.about();
    }
}
