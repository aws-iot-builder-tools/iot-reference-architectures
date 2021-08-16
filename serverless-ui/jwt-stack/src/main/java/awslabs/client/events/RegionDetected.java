package awslabs.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RegionDetected {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onRegionDetected(Event regionDetectedEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public final String region;

        public Event(String region) {
            this.region = region;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onRegionDetected(this);
        }
    }
}
