import com.awslabs.iatt.spe.serverless.gwt.server.BasicTlsHelper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.KeyPair;

public class BootstrapTest {
    public static final String RESOURCES_DIRECTORY = "src/main/resources";
    public static final String KEY_PREFIX = "fixed";
    public static final String FULL_KEY_PREFIX = String.join("/", RESOURCES_DIRECTORY, KEY_PREFIX);

    /**
     * This test bootstraps the fixed keypair
     */
    @Test
    public void bootstrapFixedKeys() {
        File publicKeyFile = new File(String.join("-", FULL_KEY_PREFIX, "public.key"));
        File privateKeyFile = new File(String.join("-", FULL_KEY_PREFIX, "private.key"));

        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            // Keys already exist, nothing to do here
            return;
        }

        BasicTlsHelper basicTlsHelper = new BasicTlsHelper();
        KeyPair randomKeypair = basicTlsHelper.getRandomKeypair();

        basicTlsHelper.writeKeyPair(FULL_KEY_PREFIX, randomKeypair);
    }
}
