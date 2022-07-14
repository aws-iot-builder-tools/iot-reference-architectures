package lambda.raspberrypi;

import awslabs.client.shared.RaspberryPiSettings;
import com.redhat.et.libguestfs.GuestFS;
import io.vavr.Function0;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.util.logging.Logger;

import static lambda.GuestFSHelper.*;
import static lambda.SharedHelpers.info;

public class OneWireEnabler implements GuestFsStepProvider<RaspberryPiSettings> {
    public static final String CONFIG_TXT = "/config.txt";
    public static final String ONE_WIRE_ENABLE_STRING_DEFAULT = "dtoverlay=w1-gpio";
    public static final Integer DEFAULT_GPIO_PIN = 4;
    private static final Logger log = Logger.getLogger(OneWireEnabler.class.getName());

    @Inject
    public OneWireEnabler() {
    }

    @Override
    public String getEnabledMessage() {
        return "Enabling 1-wire";
    }

    @Override
    public String getDisabledMessage() {
        return "Not enabling 1-wire";
    }

    @Override
    public boolean willRun(RaspberryPiSettings raspberryPiSettings) {
        return raspberryPiSettings.oneWireEnabled;
    }

    @Override
    public Void enable(GuestFS guestFS, RaspberryPiSettings raspberryPiSettings) {
        List<String> configTxtLines = readBootFileOrThrow(guestFS, CONFIG_TXT);

        if (configTxtLines.find(value -> value.startsWith(ONE_WIRE_ENABLE_STRING_DEFAULT)).isDefined()) {
            // Already enabled, nothing to do
            info("One wire already enabled");
            return null;
        }

        String enableString = ONE_WIRE_ENABLE_STRING_DEFAULT;

        int oneWirePin = Option.of(raspberryPiSettings.oneWirePinNullable).getOrElse(DEFAULT_GPIO_PIN);

        if (oneWirePin != DEFAULT_GPIO_PIN) {
            // Custom 1-wire pin requested
            enableString = String.join("", enableString, ",gpiopin=", String.valueOf(oneWirePin));
        }

        configTxtLines = configTxtLines.append(enableString);

        writeBootFile(guestFS, CONFIG_TXT, String.join("\n", configTxtLines));

        return null;
    }
}
