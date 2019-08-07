package com.awssamples.shared;

import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;

public class IotPolicies {
    private static final String AWS_IOT_DATA_ACCESS_POLICY_NAME = "AWSIoTDataAccess";
    public static final IManagedPolicy AWS_IOT_DATA_ACCESS_POLICY = ManagedPolicy.fromAwsManagedPolicyName(AWS_IOT_DATA_ACCESS_POLICY_NAME);

    private static final String AWS_IOT_CONFIG_READ_ONLY_ACCESS_POLICY_NAME = "AWSIoTConfigReadOnlyAccess";
    public static final IManagedPolicy AWS_IOT_CONFIG_READ_ONLY_ACCESS_POLICY = ManagedPolicy.fromAwsManagedPolicyName(AWS_IOT_CONFIG_READ_ONLY_ACCESS_POLICY_NAME);
}
