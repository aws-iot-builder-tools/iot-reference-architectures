package awslabs.client.ssm;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SsmConfig implements IsSerializable {
    public String url;
    public String token;

    public SsmConfig() {
    }

    public SsmConfig(String streamUrl, String tokenValue) {
        this.url = streamUrl;
        this.token = tokenValue;
    }
}
