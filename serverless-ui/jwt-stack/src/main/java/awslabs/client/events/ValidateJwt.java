package awslabs.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ValidateJwt {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onJwtValidationRequested(Event jwtValidationRequestedEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public Event() {
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onJwtValidationRequested(this);
        }
    }
}
