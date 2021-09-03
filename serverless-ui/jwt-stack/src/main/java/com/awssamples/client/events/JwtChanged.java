package com.awssamples.client.events;

import com.awssamples.client.shared.JwtCreationResponse;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class JwtChanged {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onJwtChanged(Event jwtChangedEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public final JwtCreationResponse jwtCreationResponse;

        public Event(JwtCreationResponse jwtCreationResponse) {
            this.jwtCreationResponse = jwtCreationResponse;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onJwtChanged(this);
        }
    }
}
