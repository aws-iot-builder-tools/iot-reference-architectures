package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class UpdatedSystemCount {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onUpdatedSystemCount(Event updatedSystemCountEvent);
    }

    public static class Event extends GwtEvent<UpdatedSystemCount.Handler> {
        public int count;

        public Event(int count) {
            this.count = count;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        public Event count(int count) {
            this.count = count;

            return this;
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onUpdatedSystemCount(this);
        }
    }
}
