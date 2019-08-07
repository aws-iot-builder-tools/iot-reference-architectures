package io.vertx.fargate.providers;

import io.vavr.Lazy;
import io.vertx.fargate.providers.interfaces.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;

import javax.inject.Inject;

public class BasicCredentialsProvider implements CredentialsProvider {
    private static final Logger log = LoggerFactory.getLogger(BasicCredentialsProvider.class);
    @Inject
    AwsCredentialsProviderChain awsCredentialsProviderChain;
    private Lazy<StsClient> lazyStsClient = Lazy.of(StsClient::create);

    @Inject
    public BasicCredentialsProvider() {
    }

    @Override
    public AwsSessionCredentials getAwsSessionCredentials() {
        AwsCredentials awsCredentials = awsCredentialsProviderChain.resolveCredentials();

        if (awsCredentials instanceof AwsBasicCredentials) {
            // Must be running with an IAM user, use STS to get session credentials
            GetSessionTokenResponse getSessionTokenResponse = lazyStsClient.get().getSessionToken();

            Credentials credentials = getSessionTokenResponse.credentials();
            return AwsSessionCredentials.create(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken());
        }

        return (AwsSessionCredentials) awsCredentials;
    }
}
