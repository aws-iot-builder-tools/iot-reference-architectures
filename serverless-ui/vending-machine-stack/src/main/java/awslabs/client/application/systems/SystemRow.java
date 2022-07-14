package awslabs.client.application.systems;

import awslabs.client.GwtHelpers;
import awslabs.client.shared.IotSystem;
import awslabs.client.shared.ModelWithId;
import awslabs.client.ssm.SsmHelper;
import com.google.common.io.BaseEncoding;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialCardTitle;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;

import java.nio.charset.StandardCharsets;

public class SystemRow extends Composite implements ModelWithId {
    private static final SystemRow.Binder binder = GWT.create(SystemRow.Binder.class);
    private final EventBus eventBus;
    @UiField
    MaterialCardTitle name;
    @UiField
    MaterialLabel description;
    @UiField
    MaterialLink connectLink;
    @UiField
    MaterialLink shareLink;

    private IotSystem iotSystem;

    public SystemRow(EventBus eventBus, IotSystem iotSystem) {
        initWidget(binder.createAndBindUi(this));
        this.eventBus = eventBus;
        this.iotSystem = iotSystem;
        update(iotSystem);
    }

    @UiHandler("connectLink")
    public void onConnectLinkClicked(ClickEvent clickEvent) {
        SsmHelper.connect(eventBus, iotSystem.name(), iotSystem.activationId());
    }

    @UiHandler("shareLink")
    public void onShareLinkClicked(ClickEvent clickEvent) {
        // Base64 help from - https://stackoverflow.com/a/27414325/796579
        String encodedName = BaseEncoding.base64().encode(iotSystem.name().getBytes(StandardCharsets.UTF_8));
        String encodedId = BaseEncoding.base64().encode(iotSystem.activationId().getBytes(StandardCharsets.UTF_8));
        String parameters = String.join("", "name=", encodedName, "&id=", encodedId);
        GwtHelpers.copyThis(String.join("?", GwtHelpers.getBaseUrl(), parameters));
    }

    public void update(IotSystem iotSystem) {
        this.iotSystem = iotSystem;

        name.setText(name());

        connectLink.setEnabled(iotSystem.online());
        shareLink.setEnabled(iotSystem.online());

        if (iotSystem.online()) {
            connectLink.setText("Connect");
            shareLink.setText("Share");
        } else {
            connectLink.setText("System appears to be offline");
            shareLink.setText("");
        }
    }

    @Override
    public String name() {
        return iotSystem.name();
    }

    interface Binder extends UiBinder<Widget, SystemRow> {
    }
}
