package com.awssamples.client.create;

import com.awssamples.client.IsWidget;
import com.awssamples.client.shared.JwtCreationResponse;

public interface ICreateView extends IsWidget {
    void updateIccid(String iccid);

    void updateJwtData(JwtCreationResponse jwtCreationResponse);
}
