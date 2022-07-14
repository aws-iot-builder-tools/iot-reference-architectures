package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class MqttConnectionFailed {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onMqttConnectionFailed(Event mqttConnectionFailedEvent);
    }

    public static class Event extends GwtEvent<MqttConnectionFailed.Handler> {
        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onMqttConnectionFailed(this);
        }
    }
}
