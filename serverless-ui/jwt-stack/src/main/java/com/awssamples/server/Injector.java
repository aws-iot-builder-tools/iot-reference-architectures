package com.awssamples.server;

import com.awslabs.resultsiterator.ResultsIteratorModule;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = ResultsIteratorModule.class)
public interface Injector {
    void inject(BasicJwtService basicJwtService);
}
