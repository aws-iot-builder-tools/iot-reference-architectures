package com.awssamples.client;

import com.awssamples.client.shell.ShellPresenter;
import com.awssamples.client.shell.ShellView;
import dagger.Component;

import javax.inject.Singleton;

@Component(modules = AppModule.class)
@Singleton
public interface AppInjector {
    ShellView shellView();

    void inject(ShellPresenter shellPresenter);
}
