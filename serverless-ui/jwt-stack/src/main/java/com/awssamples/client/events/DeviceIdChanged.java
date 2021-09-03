package com.awssamples.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DeviceIdChanged {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onDeviceIdChanged(Event deviceIdChangedEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public String deviceId;

        public Event(String deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onDeviceIdChanged(this);
        }
    }
}
