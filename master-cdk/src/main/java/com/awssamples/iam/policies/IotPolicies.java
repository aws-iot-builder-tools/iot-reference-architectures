package com.awssamples.iam.policies;

import software.amazon.awscdk.services.iam.ServicePrincipal;

public class IotPolicies {
    public static final ServicePrincipal IOT_SERVICE_PRINCIPAL = new ServicePrincipal("iot");
}
