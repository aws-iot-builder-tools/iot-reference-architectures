package awslabs.client.application.events;

import awslabs.client.mqtt.MqttClient;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class MqttConnectionSuccessful {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onMqttConnectionSuccessful(Event mqttConnectionSuccessfulEvent);
    }

    public static class Event extends GwtEvent<MqttConnectionSuccessful.Handler> {
        public final MqttClient mqttClient;

        public Event(MqttClient mqttClient) {
            this.mqttClient = mqttClient;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onMqttConnectionSuccessful(this);
        }
    }
}
