package awslabs.client.shared;

import awslabs.client.mqtt.ClientConfig;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface JwtServiceAsync {
    void getJwtResponse(String iccid, int expirationInMs, AsyncCallback<JwtCreationResponse> async);

    void isTokenValid(String token, AsyncCallback<JwtValidationResponse> async);

    void getClientConfig(AsyncCallback<ClientConfig> async);

    void getAuthorizerName(AsyncCallback<String> async);
}
