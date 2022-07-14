package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SystemCreated {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onSystemCreated(Event systemCreatedEvent);
    }

    public static class Event extends GwtEvent<SystemCreated.Handler> {
        public String activationId;

        public Event(String activationId) {
            this.activationId = activationId;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onSystemCreated(this);
        }
    }
}
