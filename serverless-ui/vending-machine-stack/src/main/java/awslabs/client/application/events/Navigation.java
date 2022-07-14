package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import gwt.material.design.client.ui.MaterialContainer;

public class Navigation {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onTokenChanged(Event tokenChangedEvent);
    }

    public static class Event extends GwtEvent<Navigation.Handler> {
        public String token;
        public MaterialContainer materialContainer;

        public Event(String token, MaterialContainer materialContainer) {
            this.token = token;
            this.materialContainer = materialContainer;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onTokenChanged(this);
        }
    }
}
