package io.vertx.fargate.modules;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vertx.fargate.mqtt.data.AccountId;
import io.vertx.fargate.mqtt.data.ImmutableAccountId;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sts.StsClient;

import javax.inject.Singleton;

@Module
public abstract class BaselineDaggerAwsModule {
    private static final String AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE = "AWS_ACCOUNT_ID";

    @Binds
    public abstract AwsRegionProviderChain awsRegionProviderChain(DefaultAwsRegionProviderChain defaultAwsRegionProviderChain);

    @Provides
    public static AwsCredentialsProviderChain awsCredentialsProviderChain() {
        return AwsCredentialsProviderChain.builder().build();
    }

    @Provides
    public static Region region() {
        return DefaultAwsRegionProviderChain.builder().build().getRegion();
    }

    @Provides
    public static Ec2Client ec2Client() {
        return Ec2Client.create();
    }

    @Provides
    @Singleton
    public static AccountId accountId() {
        return ImmutableAccountId.builder().accountId(getAccountId()).build();
    }

    private static String getAccountId() {
        return Option.of(System.getenv(AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE))
                .orElse(BaselineDaggerAwsModule::getAccountIdFromSts)
                .getOrElseThrow(BaselineDaggerAwsModule::throwAccountIdMissingException);
    }

    private static Option<String> getAccountIdFromSts() {
        return Try.of(() -> StsClient.create().getCallerIdentity().account()).toOption();
    }

    private static RuntimeException throwAccountIdMissingException() {
        throw new RuntimeException("Account ID is not available in the [" + AWS_ACCOUNT_ID_ENVIRONMENT_VARIABLE + "] environment variable, can not continue");
    }
}
