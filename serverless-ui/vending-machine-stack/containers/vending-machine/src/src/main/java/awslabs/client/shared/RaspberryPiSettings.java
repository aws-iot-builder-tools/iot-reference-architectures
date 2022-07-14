package awslabs.client.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.user.client.rpc.IsSerializable;
import io.vavr.control.Option;

public class RaspberryPiSettings implements IsSerializable {
    // To make sure that the hash of each request is different
    public long timestamp;
    public String imageName = null;
    public String userId = null;
    public boolean sshEnabled = true;
    public boolean oneWireEnabled = true;
    public Integer oneWirePinNullable = null;
    public String wifiSsidNullable = null;
    public String wifiPasswordNullable = null;
    public String wifiCountryCode = "US";
    public boolean ssmEnabled = true;
    public boolean addPiAccount = true;

    public RaspberryPiSettings() {
        // Required constructor for GWT-RPC serialization
        reset();
    }

    public void reset() {
        timestamp = System.currentTimeMillis();
    }

    public String getImageName() {
        return imageName;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isSshEnabled() {
        return sshEnabled;
    }

    public boolean isOneWireEnabled() {
        return oneWireEnabled;
    }

    @JsonIgnore
    public Option<Integer> getOneWirePinOption() {
        return Option.of(oneWirePinNullable);
    }

    @JsonIgnore
    public Option<String> getWifiSsidOption() {
        return Option.of(wifiSsidNullable);
    }

    @JsonIgnore
    public Option<String> getWifiPasswordOption() {
        return Option.of(wifiPasswordNullable);
    }

    public String getWifiCountryCode() {
        return wifiCountryCode;
    }

    public boolean isSsmEnabled() {
        return ssmEnabled;
    }

    public boolean isAddPiAccount() {
        return addPiAccount;
    }
}
