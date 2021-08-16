package awslabs.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class MqttMessage {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onMqttMessage(Event mqttMessageEvent);
    }

    public static class Event extends GwtEvent<Handler> {
        public final String topic;
        public final String payload;

        public Event(String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onMqttMessage(this);
        }
    }
}
