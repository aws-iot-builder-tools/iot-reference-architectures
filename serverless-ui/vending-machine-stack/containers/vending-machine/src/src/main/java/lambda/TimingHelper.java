package lambda;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

import static lambda.SharedHelpers.info;

public class TimingHelper {
    private static Void time(String message, CheckedRunnable runnable) {
        return TimingHelper.timeTry(message, () -> Try.run(runnable));
    }

    private static <T> T time(String message, CheckedFunction0<? extends T> supplier) {
        return TimingHelper.timeTry(message, () -> Try.of(supplier));
    }

    private static <T> T timeTry(String message, Callable<Try<T>> callable) {
        info("START " + message);
        Instant start = Instant.now();
        Try<T> result = Try.of(callable::call).get();
        Instant end = Instant.now();
        info("END " + message + " [" + (result.isSuccess() ? "SUCCESS" : "FAILURE") + " [" + Duration.between(start, end) + "]");

        return result.get();
    }
}
