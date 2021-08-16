package awslabs.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class IccidChanged {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onIccidChanged(Event iccidChangedEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public String iccid;

        public Event(String iccid) {
            this.iccid = iccid;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onIccidChanged(this);
        }
    }
}
