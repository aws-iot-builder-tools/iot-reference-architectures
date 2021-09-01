package com.awssamples.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RequestJwt {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onJwtRequested(Event jwtRequestedEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public String iccid;
        public int expirationTimeInSeconds;

        public Event(String iccid, int expirationTimeInSeconds) {
            this.iccid = iccid;
            this.expirationTimeInSeconds = expirationTimeInSeconds;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onJwtRequested(this);
        }
    }
}
