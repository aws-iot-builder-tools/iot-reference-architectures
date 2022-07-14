package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BuildRequestSuccessful {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onBuildRequestSuccessful(Event buildRequestSuccessfulEvent);
    }

    public static class Event extends GwtEvent<BuildRequestSuccessful.Handler> {
        public String buildId;

        public Event(String buildId) {
            this.buildId = buildId;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onBuildRequestSuccessful(this);
        }
    }
}
