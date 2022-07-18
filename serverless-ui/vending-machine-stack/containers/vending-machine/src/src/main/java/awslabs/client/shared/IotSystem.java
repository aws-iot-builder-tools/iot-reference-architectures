package awslabs.client.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class IotSystem implements IsSerializable, ModelWithId {
    private String name;
    private String activationId;

    private boolean online = false;

    public IotSystem() {
    }

    public IotSystem(String name, String activationId) {
        this.name = name;
        this.activationId = activationId;
    }

    public String name() {
        return name;
    }

    public boolean online() {
        return online;
    }

    public String activationId() {
        return activationId;
    }

    public IotSystem online(boolean online) {
        this.online = online;

        return this;
    }
}
