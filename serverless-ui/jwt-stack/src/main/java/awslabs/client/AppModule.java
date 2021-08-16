package awslabs.client;

import awslabs.client.attribution.AttributionPresenter;
import awslabs.client.attribution.AttributionView;
import awslabs.client.attribution.IAttributionPresenter;
import awslabs.client.attribution.IAttributionView;
import awslabs.client.create.CreatePresenter;
import awslabs.client.create.CreateView;
import awslabs.client.create.ICreatePresenter;
import awslabs.client.create.ICreateView;
import awslabs.client.shell.IShellView;
import awslabs.client.shell.ShellView;
import awslabs.client.test.ITestPresenter;
import awslabs.client.test.ITestView;
import awslabs.client.test.TestPresenter;
import awslabs.client.test.TestView;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import java.util.logging.Logger;

@Module
public abstract class AppModule {
    @Provides
    @Singleton
    public static EventBus eventBus() {
        return GWT.create(SimpleEventBus.class);
    }

    @Provides
    @Singleton
    public static ICreatePresenter createPresenter(CreatePresenter createPresenter) {
        return navigationHandlerSetup(createPresenter);
    }

    @Provides
    @Singleton
    public static IAttributionPresenter attributionPresenter(AttributionPresenter attributionPresenter) {
        return navigationHandlerSetup(attributionPresenter);
    }

    @Provides
    @Singleton
    public static ITestPresenter testPresenter(TestPresenter testPresenter) {
        return navigationHandlerSetup(testPresenter);
    }

    @Provides
    @Singleton
    public static Logger logger() {
        return Logger.getLogger("");
    }

    private static <T extends ReceivesEvents> T bindEventBus(T receivesEvents) {
        receivesEvents.bindEventBus();

        return receivesEvents;
    }

    private static <T extends MainNavigationHandler> T navigationHandlerSetup(T mainNavigationHandler) {
        mainNavigationHandler.navigationHandlerSetup();

        return mainNavigationHandler;
    }

    @Binds
    @Singleton
    public abstract ICreateView createView(CreateView createView);

    @Binds
    @Singleton
    public abstract IAttributionView attributionView(AttributionView attributionView);

    @Binds
    @Singleton
    public abstract IShellView shellView(ShellView shellView);

    @Binds
    @Singleton
    public abstract ITestView testView(TestView testView);
}
