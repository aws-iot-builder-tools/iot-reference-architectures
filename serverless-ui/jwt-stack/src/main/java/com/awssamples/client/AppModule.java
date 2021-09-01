package com.awssamples.client;

import com.awssamples.client.attribution.AttributionPresenter;
import com.awssamples.client.attribution.AttributionView;
import com.awssamples.client.attribution.IAttributionPresenter;
import com.awssamples.client.attribution.IAttributionView;
import com.awssamples.client.create.CreatePresenter;
import com.awssamples.client.create.CreateView;
import com.awssamples.client.create.ICreatePresenter;
import com.awssamples.client.create.ICreateView;
import com.awssamples.client.shell.IShellView;
import com.awssamples.client.shell.ShellView;
import com.awssamples.client.test.ITestPresenter;
import com.awssamples.client.test.ITestView;
import com.awssamples.client.test.TestPresenter;
import com.awssamples.client.test.TestView;
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
