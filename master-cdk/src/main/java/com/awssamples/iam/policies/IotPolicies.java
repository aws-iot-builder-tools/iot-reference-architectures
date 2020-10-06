package com.awssamples.iam.policies;

import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.Collections;

import static com.awssamples.iam.Permissions.ALL_RESOURCES;
import static com.awssamples.iam.Permissions.IOT_SEARCH_INDEX_PERMISSION;

public class IotPolicies {
    public static final ServicePrincipal IOT_SERVICE_PRINCIPAL = new ServicePrincipal("iot");

    public static PolicyStatement getSearchIndexPolicy() {
        PolicyStatementProps sqsPolicyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .resources(Collections.singletonList(ALL_RESOURCES))
                .actions(Collections.singletonList(IOT_SEARCH_INDEX_PERMISSION))
                .build();

        return new PolicyStatement(sqsPolicyStatementProps);
    }
}
