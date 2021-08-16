package awslabs.client.attribution;

import awslabs.client.events.DeviceIdChanged;
import awslabs.client.events.IccidChanged;
import awslabs.client.events.RequestJwt;
import awslabs.client.events.ValidateJwt;
import awslabs.client.place.NameTokens;
import awslabs.client.shared.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialLoader;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
