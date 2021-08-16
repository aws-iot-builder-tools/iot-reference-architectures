package awslabs.client.create;

import awslabs.client.IsWidget;
import awslabs.client.shared.JwtCreationResponse;

public interface ICreateView extends IsWidget {
    void updateIccid(String iccid);

    void updateJwtData(JwtCreationResponse jwtCreationResponse);
}
