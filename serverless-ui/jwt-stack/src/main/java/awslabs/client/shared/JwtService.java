package awslabs.client.shared;

import awslabs.client.mqtt.ClientConfig;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("jwt")
public interface JwtService extends RemoteService {
    int EXPIRATION_IN_MS_MIN = 10000;
    int EXPIRATION_IN_MS_MAX = 120000;
    int EXPIRATION_IN_SECONDS_MIN = EXPIRATION_IN_MS_MIN / 1000;
    int EXPIRATION_IN_SECONDS_MAX = EXPIRATION_IN_MS_MAX / 1000;

    JwtCreationResponse getJwtResponse(String iccid, int expirationInMs);

    JwtValidationResponse isTokenValid(String token);

    ClientConfig getClientConfig();

    String getAuthorizerName();
}
