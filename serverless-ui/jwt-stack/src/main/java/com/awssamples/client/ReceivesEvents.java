package com.awssamples.client;

import com.google.gwt.event.shared.EventBus;

public interface ReceivesEvents {
    void bindEventBus();

    EventBus getEventBus();
}
