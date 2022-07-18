package awslabs.client;

import awslabs.client.mqtt.ClientConfig;
import awslabs.client.shared.IotBuild;
import awslabs.client.shared.IotSystem;
import awslabs.client.shared.RaspberryPiRequest;
import awslabs.client.ssm.SsmConfig;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("iot")
public interface IotService extends RemoteService {
    ClientConfig getClientConfig(String userIdNullable);

    List<IotBuild> getBuildList(String userId);

    List<IotSystem> getSystemList(String userId);

    String getPresignedS3Url(String buildId);

    String buildImage(RaspberryPiRequest raspberryPiRequest);

    SsmConfig getSessionManagerConfig(String activationId);

    String getSessionManagerUrl(String activationId);
}
