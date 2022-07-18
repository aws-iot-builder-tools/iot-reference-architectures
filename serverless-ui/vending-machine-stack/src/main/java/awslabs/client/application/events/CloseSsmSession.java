package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CloseSsmSession {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onCloseSsmSession(Event closeSsmSessionEvent);
    }

    public static class Event extends GwtEvent<CloseSsmSession.Handler> {
        public final String sessionName;

        public Event(String sessionName) {
            this.sessionName = sessionName;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onCloseSsmSession(this);
        }
    }
}
