package awslabs.client.ssm;

import awslabs.client.IotService;
import awslabs.client.IotServiceAsync;
import awslabs.client.application.events.NewSsmSession;
import awslabs.client.application.events.ShowMaterialLoader;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import gwt.material.design.client.ui.MaterialLoader;

import static awslabs.client.application.shared.GwtHelper.info;

public class SsmHelper {
    public static final IotServiceAsync IOT_SERVICE_ASYNC = GWT.create(IotService.class);

    public static void connect(EventBus eventBus, String iotSystemName, String ssmActivationId) {
        MaterialLoader.loading(true);
        eventBus.fireEvent(new ShowMaterialLoader.Event(true));

        IOT_SERVICE_ASYNC.getSessionManagerConfig(ssmActivationId, new AsyncCallback<SsmConfig>() {
            @Override
            public void onFailure(Throwable caught) {
                eventBus.fireEvent(new ShowMaterialLoader.Event(false));
                MaterialLoader.loading(false);
                info("Failed to get the session manager configuration information, can not connect");
            }

            @Override
            public void onSuccess(SsmConfig ssmConfig) {
                eventBus.fireEvent(new ShowMaterialLoader.Event(false));

                if (ssmConfig == null) {
                    info("Unable to connect to that host at the moment, try again later");
                    return;
                }

                MaterialLoader.loading(false);

                SsmWebSocket ssmWebSocket = new SsmWebSocket(eventBus, iotSystemName, ssmConfig);
                eventBus.fireEvent(new NewSsmSession.Event(ssmWebSocket));
            }
        });
    }
}
