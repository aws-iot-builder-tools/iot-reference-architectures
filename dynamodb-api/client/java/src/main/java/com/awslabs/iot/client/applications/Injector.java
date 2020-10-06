package com.awslabs.iot.client.applications;

import com.awslabs.iot.client.interfaces.DynamoDbApiClientTerminal;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = DynamoDbApiClientModule.class)
public interface Injector {
    DynamoDbApiClientTerminal DynamoDbApiClientTerminal();

    DynamoDbApiClientConsole DynamoDbApiClientConsole();

    Arguments arguments();
}

