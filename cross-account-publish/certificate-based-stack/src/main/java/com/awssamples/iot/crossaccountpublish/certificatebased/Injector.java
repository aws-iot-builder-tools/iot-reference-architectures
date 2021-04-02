package com.awssamples.iot.crossaccountpublish.certificatebased;

import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.v2.V2HelperModule;
import dagger.Component;
import software.amazon.awssdk.services.iot.IotClient;

import javax.inject.Singleton;

@Singleton
@Component(modules = {V2HelperModule.class})
public interface Injector {
    V2IotHelper v2IotHelper();
}
