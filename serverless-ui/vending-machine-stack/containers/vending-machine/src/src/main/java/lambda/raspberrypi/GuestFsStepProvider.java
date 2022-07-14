package lambda.raspberrypi;

import com.redhat.et.libguestfs.GuestFS;
import io.vavr.Function0;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;

import static lambda.SharedHelpers.info;

public interface GuestFsStepProvider<T> {
    String getEnabledMessage();

    String getDisabledMessage();

    /**
     * Generates steps to update a guest filesystem based on some input
     *
     * @param guestFS the guest filesystem to be updated
     * @param input   the input (e.g. RaspberryPiSettings) used to generate the steps
     * @return Option.some(Lazy < Void >) which will generate the steps on demand, Option.none() if no steps are required
     */
    default Option<Tuple2<String, Function0<Void>>> getSteps(GuestFS guestFS, T input) {
        if (!willRun(input)) {
            // Not running, show the disabled message and return nothing
            info(getDisabledMessage());
            return Option.none();
        }

        // Running, show the enabled message
        info(getEnabledMessage());

        // Wrap up the enable function in an optional
        return Option.of(Function0.of(() -> enable(guestFS, input)))
                // Add the enabled message to the steps so it can be printed out inline later
                .map(steps -> Tuple.of(getEnabledMessage(), steps));
    }

    Void enable(GuestFS guestFS, T input);

    boolean willRun(T input);
}
