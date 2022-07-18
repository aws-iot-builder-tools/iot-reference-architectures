package awslabs.client.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RaspberryPiRequest implements IsSerializable {
    public boolean dryRun = true;
    public RaspberryPiSettings settings = new RaspberryPiSettings();

    public RaspberryPiRequest() {
        // Required constructor for GWT-RPC serialization
    }
}
