package com.awssamples.crossaccountpublish;

import com.awslabs.resultsiterator.ResultsIteratorModule;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ResultsIteratorModule.class})
public interface Injector {
    void inject(CertificateBasedStack certificateBasedStack);
}
