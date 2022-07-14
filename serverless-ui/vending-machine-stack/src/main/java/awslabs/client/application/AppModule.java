package awslabs.client.application;

import awslabs.client.application.about.AboutPresenter;
import awslabs.client.application.about.AboutView;
import awslabs.client.application.about.IAboutPresenter;
import awslabs.client.application.about.IAboutView;
import awslabs.client.application.builds.BuildsPresenter;
import awslabs.client.application.builds.BuildsView;
import awslabs.client.application.builds.IBuildsPresenter;
import awslabs.client.application.builds.IBuildsView;
import awslabs.client.application.raspberrypi.IRaspberryPiPresenter;
import awslabs.client.application.raspberrypi.IRaspberryPiView;
import awslabs.client.application.raspberrypi.RaspberryPiPresenter;
import awslabs.client.application.raspberrypi.RaspberryPiView;
import awslabs.client.application.shared.MainNavigationHandler;
import awslabs.client.application.shared.ReceivesEvents;
import awslabs.client.application.shell.IShellView;
import awslabs.client.application.shell.ShellView;
import awslabs.client.application.systems.ISystemsPresenter;
import awslabs.client.application.systems.ISystemsView;
import awslabs.client.application.systems.SystemsPresenter;
import awslabs.client.application.systems.SystemsView;
import awslabs.client.application.terminals.ITerminalsPresenter;
import awslabs.client.application.terminals.ITerminalsView;
import awslabs.client.application.terminals.TerminalsPresenter;
import awslabs.client.application.terminals.TerminalsView;
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
    public static IAboutPresenter aboutPresenter(AboutPresenter aboutPresenter) {
        return navigationHandlerSetup(aboutPresenter);
    }

    @Provides
    @Singleton
    public static IBuildsPresenter buildsPresenter(BuildsPresenter buildsPresenter) {
        return navigationHandlerSetup(buildsPresenter);
    }

    @Provides
    @Singleton
    public static IRaspberryPiPresenter raspberryPiPresenter(RaspberryPiPresenter raspberryPiPresenter) {
        return navigationHandlerSetup(raspberryPiPresenter);
    }

    @Provides
    @Singleton
    public static ISystemsPresenter systemsPresenter(SystemsPresenter systemsPresenter) {
        return navigationHandlerSetup(systemsPresenter);
    }

    @Provides
    @Singleton
    public static ITerminalsPresenter terminalsPresenter(TerminalsPresenter terminalsPresenter) {
        return navigationHandlerSetup(terminalsPresenter);
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
    public abstract IAboutView aboutView(AboutView aboutView);

    @Binds
    @Singleton
    public abstract IShellView applicationView(ShellView applicationView);

    @Binds
    @Singleton
    public abstract IBuildsView buildsView(BuildsView buildsView);

    @Binds
    @Singleton
    public abstract IRaspberryPiView raspberryPiView(RaspberryPiView raspberryPiView);

    @Binds
    @Singleton
    public abstract ISystemsView systemsView(SystemsView systemsView);

    @Binds
    @Singleton
    public abstract ITerminalsView terminalsView(TerminalsView terminalsView);
}
