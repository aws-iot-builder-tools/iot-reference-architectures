package com.awssamples.client.mqtt.callbacks;

import jsinterop.annotations.JsFunction;

@JsFunction
public interface ReconnectCallback {
    void onReconnect();
}
