package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class MqttConnectionAttempt {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onMqttConnectionAttempt(Event mqttConnectionAttemptEvent);
    }

    public static class Event extends GwtEvent<MqttConnectionAttempt.Handler> {
        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onMqttConnectionAttempt(this);
        }
    }
}
