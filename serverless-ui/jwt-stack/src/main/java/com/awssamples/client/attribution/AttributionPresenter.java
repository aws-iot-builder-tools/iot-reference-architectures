package com.awssamples.client.attribution;

import com.awssamples.client.place.NameTokens;
import com.awssamples.client.shared.*;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Widget;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AttributionPresenter implements IAttributionPresenter {
    @Inject
    EventBus eventBus;

    @Inject
    IAttributionView createView;

    @Inject
    Logger log;

    @Inject
    AttributionPresenter() {
    }

    @Inject
    public void setup() {
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
        return createView.getWidget();
    }

    @Override
    public String getToken() {
        return NameTokens.attribution();
    }
}
