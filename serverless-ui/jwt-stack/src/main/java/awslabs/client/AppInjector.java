package awslabs.client;

import awslabs.client.shell.ShellPresenter;
import awslabs.client.shell.ShellView;
import dagger.Component;

import javax.inject.Singleton;

@Component(modules = AppModule.class)
@Singleton
public interface AppInjector {
    ShellView shellView();

    void inject(ShellPresenter shellPresenter);
}
