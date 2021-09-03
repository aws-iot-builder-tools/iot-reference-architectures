package com.awssamples.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import io.vavr.control.Option;

public class AttributionChanged {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onAttributionChanged(Event attributionChangedEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public final Option<String> attributionStringOption;

        public Event(Option<String> attributionStringOption) {
            this.attributionStringOption = attributionStringOption;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onAttributionChanged(this);
        }
    }
}
