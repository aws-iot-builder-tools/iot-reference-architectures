package awslabs.client.application.shared;

import awslabs.client.application.events.Navigation;

public interface MainNavigationHandler extends Navigation.Handler, IsWidget, ReceivesEvents {
    String getToken();

    default void navigationHandlerSetup() {
        bindEventBus();
        getEventBus().addHandler(Navigation.TYPE, this::onTokenChanged);
    }

    default boolean tokenMatches(String otherToken) {
        // This is a separate method so it can be overridden with other behavior (partial matching, etc)
        return getToken().equals(otherToken);
    }

    @Override
    default void onTokenChanged(Navigation.Event tokenChangedEvent) {
        if (!tokenMatches(tokenChangedEvent.token)) {
            // Not the right token, do nothing
            return;
        }

        tokenChangedEvent.materialContainer.clear();
        tokenChangedEvent.materialContainer.add(getWidget());
    }
}
