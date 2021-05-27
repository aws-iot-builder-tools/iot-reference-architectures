package com.awssamples;

import com.awslabs.resultsiterator.ResultsIteratorModule;
import com.awssamples.amazon_ion_handler.AmazonIonHandlerStack;
import com.awssamples.cross_account_publish.CertificateBasedStack;
import com.awssamples.dynamodb_api.DynamoDbApiDependentStack;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ResultsIteratorModule.class})
public interface MasterInjector {
    void inject(AmazonIonHandlerStack amazonIonHandlerStack);

    void inject(CertificateBasedStack certificateBasedStack);

    void inject(DynamoDbApiDependentStack dynamoDbApiDependentStack);
}
