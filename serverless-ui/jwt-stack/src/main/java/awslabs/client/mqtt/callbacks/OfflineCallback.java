package awslabs.client.mqtt.callbacks;

import jsinterop.annotations.JsFunction;

@JsFunction
public interface OfflineCallback {
    void onOffline();
}
