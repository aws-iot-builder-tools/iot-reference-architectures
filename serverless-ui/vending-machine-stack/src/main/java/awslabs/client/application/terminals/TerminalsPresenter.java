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
package awslabs.client.application.terminals;

import awslabs.client.application.events.NewSsmSession;
import awslabs.client.application.events.NewTerminalWidget;
import awslabs.client.application.terminals.terminal.TerminalWidget;
import awslabs.client.place.NameTokens;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Widget;

import javax.inject.Inject;
import java.util.logging.Logger;

public class TerminalsPresenter implements ITerminalsPresenter {
    @Inject
    EventBus eventBus;
    @Inject
    ITerminalsView terminalsView;
    @Inject
    Logger log;

    @Inject
    TerminalsPresenter() {
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(NewSsmSession.TYPE, this::onNewSsmSession);
    }

    private void onNewSsmSession(NewSsmSession.Event event) {
        TerminalWidget terminalWidget = new TerminalWidget(eventBus, event.ssmWebSocket);

        eventBus.fireEvent(new NewTerminalWidget.Event(terminalWidget));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Widget getWidget() {
        return terminalsView.getWidget();
    }

    @Override
    public String getToken() {
        return NameTokens.terminals();
    }
}
