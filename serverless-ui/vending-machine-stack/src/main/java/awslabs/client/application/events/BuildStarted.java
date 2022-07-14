package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import java.util.Date;

public class BuildStarted {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onBuildStarted(Event buildStartedEvent);
    }

    public static class Event extends GwtEvent<BuildStarted.Handler> {
        public String buildId;
        public Date timeStarted;

        public Event(String buildId) {
            this.buildId = buildId;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        public Event timeStarted(Date timeStarted) {
            this.timeStarted = timeStarted;

            return this;
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onBuildStarted(this);
        }
    }
}
