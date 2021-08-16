package awslabs.client.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

@SuppressWarnings("serial")
public class JwtValidationResponse extends NoToString implements IsSerializable {
    public boolean valid;

    public String errorMessage;

    public JwtValidationResponse valid(boolean valid) {
        this.valid = valid;
        return this;
    }

    public JwtValidationResponse errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }
}
