package awslabs.client;

import awslabs.client.mqtt.ClientConfig;
import awslabs.client.shared.IotBuild;
import awslabs.client.shared.IotSystem;
import awslabs.client.shared.RaspberryPiRequest;
import awslabs.client.ssm.SsmConfig;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

public interface IotServiceAsync {
    void getClientConfig(String userIdNullable, AsyncCallback<ClientConfig> async);

    void buildImage(RaspberryPiRequest raspberryPiRequest, AsyncCallback<String> async);

    void getPresignedS3Url(String buildId, AsyncCallback<String> async);

    void getSessionManagerUrl(String activationId, AsyncCallback<String> async);

    void getBuildList(String userId, AsyncCallback<List<IotBuild>> buildList);

    void getSystemList(String userId, AsyncCallback<List<IotSystem>> systemList);

    void getSessionManagerConfig(String activationId, AsyncCallback<SsmConfig> async);
}
