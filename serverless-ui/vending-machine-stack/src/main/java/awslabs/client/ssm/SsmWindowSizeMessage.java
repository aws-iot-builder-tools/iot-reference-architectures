package awslabs.client.ssm;

import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.google.gwt.core.client.GWT;
import io.vavr.Lazy;

public class SsmWindowSizeMessage {
    // This is lazy so that this class can be used in tests without throwing a GWT.create error
    private static Lazy<SsmWindowSizeMessageMapper> mapperLazy = Lazy.of(() -> GWT.create(SsmWindowSizeMessageMapper.class));
    public int cols = 137;
    public int rows = 14;
    public SsmWindowSizeMessage() {
    }

    public String toJson() {
        return mapperLazy.get().write(this);
    }

    public interface SsmWindowSizeMessageMapper extends ObjectMapper<SsmWindowSizeMessage> {
    }
}
