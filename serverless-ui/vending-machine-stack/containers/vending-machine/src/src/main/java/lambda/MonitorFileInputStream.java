package lambda;

import com.google.common.util.concurrent.RateLimiter;
import io.vavr.Lazy;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.io.*;

import static lambda.SharedHelpers.publishBuildProgress;

public class MonitorFileInputStream extends FileInputStream {
    private File file = null;
    private int position = 0;
    private final Lazy<Option<Long>> lazySize = Lazy.of(() -> Try.of(() -> file.length()).toOption());
    private RateLimiter rateLimiter = RateLimiter.create(1);

    public MonitorFileInputStream(@NotNull File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    public MonitorFileInputStream(@NotNull String name) throws FileNotFoundException {
        super(name);
        throw new RuntimeException("Named file support has not been implemented");
    }

    public MonitorFileInputStream(@NotNull FileDescriptor fdObj) {
        super(fdObj);
        throw new RuntimeException("File descriptor support has not been implemented");
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        int bytesRead = super.read(b);

        position += bytesRead;

        updateBuildProgress();

        return bytesRead;
    }


    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);

        position += bytesRead;

        updateBuildProgress();

        return bytesRead;
    }

    private void updateBuildProgress() {
        int progress = lazySize.get()
                .map(value -> (int) (((double) position / (double) value) * 100.0))
                .getOrElse(0);

        if (rateLimiter.tryAcquire(1)) {
            publishBuildProgress(progress);
        }
    }
}
