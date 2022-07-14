package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class UserIdChanged {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onUserIdChanged(Event UserIdChangedEvent);
    }

    public static class Event extends GwtEvent<UserIdChanged.Handler> {
        public String userId;

        public Event(String userId) {
            this.userId = userId;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onUserIdChanged(this);
        }
    }
}
