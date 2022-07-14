package awslabs.client.application.events;

import awslabs.client.ssm.SsmWebSocket;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class NewSsmSession {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onNewSsmSession(Event newSsmSessionEvent);
    }

    public static class Event extends GwtEvent<NewSsmSession.Handler> {
        public final SsmWebSocket ssmWebSocket;

        public Event(SsmWebSocket ssmWebSocket) {
            this.ssmWebSocket = ssmWebSocket;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onNewSsmSession(this);
        }
    }
}
