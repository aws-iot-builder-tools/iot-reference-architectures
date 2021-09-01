package com.awssamples.iot.crossaccountpublish.certificatebased;

import com.awslabs.iot.helpers.interfaces.IotHelper;
import com.awslabs.resultsiterator.ResultsIteratorModule;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ResultsIteratorModule.class})
public interface Injector {
    IotHelper iotHelper();
}
