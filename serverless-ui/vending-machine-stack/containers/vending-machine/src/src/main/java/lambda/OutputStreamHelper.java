package lambda;

import io.vavr.control.Try;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class OutputStreamHelper {
    public static final String NEWLINE = "\n";
    private static final Logger log = Logger.getLogger(App.class.getName());

    private static void print(OutputStream outputStream, String message) {
        if (!message.equals(NEWLINE)) {
            log.info(message);
        }

        Try.run(() -> outputStream.write(message.getBytes(StandardCharsets.UTF_8))).get();
    }

    public static void println(OutputStream outputStream, String message) {
        print(outputStream, message);
    }
}
