package awslabs.client.create;

import awslabs.client.events.*;
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

public class CreatePresenter implements ICreatePresenter {
    private static final JwtServiceAsync JWT_SERVICE_ASYNC = GWT.create(JwtService.class);
    @Inject
    EventBus eventBus;

    @Inject
    ICreateView createView;

    @Inject
    Logger log;
    private Option<JwtCreationResponse> jwtResponseOption = Option.none();
    private Option<String> attributionStringOption = Option.none();

    @Inject
    CreatePresenter() {
    }

    @Inject
    public void setup() {
        calculateIccid("");
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(DeviceIdChanged.TYPE, CreatePresenter.this::onDeviceIdChanged);
        eventBus.addHandler(RequestJwt.TYPE, CreatePresenter.this::onJwtRequested);
        eventBus.addHandler(ValidateJwt.TYPE, CreatePresenter.this::onJwtValidationRequested);
        eventBus.addHandler(AttributionChanged.TYPE, CreatePresenter.this::onAttributionChanged);
    }

    private void onAttributionChanged(AttributionChanged.Event event) {
        attributionStringOption = event.attributionStringOption;
    }

    private void onJwtRequested(RequestJwt.Event event) {
        MaterialLoader.loading(true, "Requesting the JWT...");

        JWT_SERVICE_ASYNC.getJwtResponse(event.iccid, event.expirationTimeInSeconds * 1000, new AsyncCallback<JwtCreationResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                MaterialLoader.loading(false);
                MaterialToast.fireToast("FAIL [" + caught.getLocalizedMessage() + "]");
            }

            @Override
            public void onSuccess(JwtCreationResponse result) {
                MaterialLoader.loading(false);
                jwtResponseOption = Option.of(result);
                createView.updateJwtData(result);
                eventBus.fireEvent(new JwtChanged.Event(result));
            }
        });
    }

    private void onJwtValidationRequested(ValidateJwt.Event event) {
        if (jwtResponseOption.isEmpty()) {
            MaterialToast.fireToast("No JWT generated yet");
            return;
        }

        String tokenWithSignature = Helpers.getTokenWithSignature(jwtResponseOption.get());

        MaterialLoader.loading(true, "Validating the JWT...");

        JWT_SERVICE_ASYNC.isTokenValid(tokenWithSignature, new AsyncCallback<JwtValidationResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                MaterialLoader.loading(false);

                MaterialToast.fireToast("FAIL [" + caught.getLocalizedMessage() + "]");
            }

            @Override
            public void onSuccess(JwtValidationResponse result) {
                MaterialLoader.loading(false);

                if (result.valid) {
                    MaterialToast.fireToast("JWT is valid");
                } else {
                    MaterialToast.fireToast("JWT is not valid [" + result.errorMessage + "]");
                }
            }
        });
    }

    private void onDeviceIdChanged(DeviceIdChanged.Event event) {
        calculateIccid(event.deviceId);
    }

    private void calculateIccid(String deviceId) {
        Try<String> newIccid = Try.of(() -> new Random(deviceId.hashCode()))
                // Make sure the value is large enough to fill our string
                .map(Random::nextLong)
                .map(BigInteger::valueOf)
                .map(BigInteger::abs)
                .map(value -> value.add(new BigInteger("77777777777777777777")))
                .map(value -> value.pow(2))
                .map(value -> value.toString(10))
                // Trim the string length to a max of 19 characters
                .map(value -> value.substring(0, 19))
                // If anything fails just show a toast message
                .onFailure(throwable -> MaterialToast.fireToast("Failed to generate a new ICCID [" + throwable.getMessage() + "]"));

        // Update our view's ICCID field
        newIccid.forEach(createView::updateIccid);

        // Create an ICCID changed event
        newIccid.map(IccidChanged.Event::new)
                // Send it to anyone else who needs to know about the ICCID change
                .forEach(newEvent -> eventBus.fireEvent(newEvent));
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
        return NameTokens.createAndValidate();
    }
}
