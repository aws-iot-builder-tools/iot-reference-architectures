package awslabs.client.application;

import awslabs.client.application.shell.ShellPresenter;
import awslabs.client.application.shell.ShellView;
import dagger.Component;

import javax.inject.Singleton;

@Component(modules = AppModule.class)
@Singleton
public interface AppInjector {
    ShellView applicationView();

    void inject(ShellPresenter shellPresenter);
}
