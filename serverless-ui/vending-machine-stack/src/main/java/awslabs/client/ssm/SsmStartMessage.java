package awslabs.client.ssm;

import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.google.gwt.core.client.GWT;
import io.vavr.Lazy;

public class SsmStartMessage {
    // This is lazy so that this class can be used in tests without throwing a GWT.create error
    private static final Lazy<SsmStartMessageMapper> mapperLazy = Lazy.of(() -> GWT.create(SsmStartMessageMapper.class));
    public String MessageSchemaVersion = "1.0";
    public String TokenValue;
    public SsmStartMessage(String tokenValue) {
        this.TokenValue = tokenValue;
    }

    public String toJson() {
        return mapperLazy.get().write(this);
    }

    public interface SsmStartMessageMapper extends ObjectMapper<SsmStartMessage> {
    }
}
