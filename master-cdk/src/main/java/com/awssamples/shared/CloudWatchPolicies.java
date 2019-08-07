package com.awssamples.shared;

import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;

public class CloudWatchPolicies {
    private static final String CLOUD_WATCH_FULL_ACCESS_POLICY_NAME = "CloudWatchFullAccess";
    public static final IManagedPolicy CLOUD_WATCH_FULL_ACCESS_POLICY = ManagedPolicy.fromAwsManagedPolicyName(CLOUD_WATCH_FULL_ACCESS_POLICY_NAME);
}
