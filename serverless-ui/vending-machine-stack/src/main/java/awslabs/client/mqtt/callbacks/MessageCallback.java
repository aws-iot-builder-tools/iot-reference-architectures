package awslabs.client.mqtt.callbacks;

import jsinterop.annotations.JsFunction;

@JsFunction
public interface MessageCallback {
    void onMessage(String topic, Object payload);
}
