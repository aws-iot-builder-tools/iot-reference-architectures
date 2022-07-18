package awslabs.client.application.events;

import awslabs.client.shared.RaspberryPiRequest;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BuildRequestedByUser {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onBuildRequestedByUser(Event buildRequestedByUser);
    }

    public static class Event extends GwtEvent<BuildRequestedByUser.Handler> {
        public RaspberryPiRequest raspberryPiRequest;

        public Event(RaspberryPiRequest raspberryPiRequest) {
            this.raspberryPiRequest = raspberryPiRequest;

            // Make sure each request is unique by resetting the timestamp field
            this.raspberryPiRequest.settings.reset();
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onBuildRequestedByUser(this);
        }
    }
}
