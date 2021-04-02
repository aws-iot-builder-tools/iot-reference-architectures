package com.awssamples;

import com.awslabs.resultsiterator.v2.V2HelperModule;
import com.awssamples.amazon_ion_handler.AmazonIonHandlerStack;
import com.awssamples.cross_account_publish.CertificateBasedStack;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {V2HelperModule.class})
public interface MasterInjector {
    void inject(AmazonIonHandlerStack amazonIonHandlerStack);

    void inject(CertificateBasedStack certificateBasedStack);
}
