package lambda;

import awslabs.client.shared.RaspberryPiSettings;
import com.redhat.et.libguestfs.GuestFS;
import io.vavr.Function0;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lambda.raspberrypi.GuestFsStepProvider;

import javax.inject.Inject;
import java.util.Set;
import java.util.logging.Logger;

public class RaspberryPiSettingsProcessor {
    private static final Logger log = Logger.getLogger(RaspberryPiSettingsProcessor.class.getName());

    @Inject
    Set<GuestFsStepProvider<RaspberryPiSettings>> raspberryPiGuestFsStepProviderSet;

    @Inject
    public RaspberryPiSettingsProcessor() {
    }

    private static Option<Tuple2<String, Function0<Void>>> getSteps(GuestFS guestFS, RaspberryPiSettings raspberryPiSettings, GuestFsStepProvider<RaspberryPiSettings> guestFsStepProvider) {
        return guestFsStepProvider.getSteps(guestFS, raspberryPiSettings);
    }

    public List<Tuple2<String, Function0<Void>>> getSteps(GuestFS guestFS, RaspberryPiSettings raspberryPiSettings) {
        return HashSet.ofAll(raspberryPiGuestFsStepProviderSet)
                .flatMap(value -> getSteps(guestFS, raspberryPiSettings, value))
                .toList();
    }
}
