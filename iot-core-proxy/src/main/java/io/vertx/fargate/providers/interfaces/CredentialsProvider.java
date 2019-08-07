package io.vertx.fargate.providers.interfaces;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public interface CredentialsProvider {
    AwsSessionCredentials getAwsSessionCredentials();
}
