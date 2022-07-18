package lambda;

import io.vavr.control.Try;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class SpyInputStream extends FileInputStream {
    private final int size = Try.of(this::available).get();

    public SpyInputStream(String name) throws FileNotFoundException {
        super(name);
    }

    public double progress0to1() {
        return ((double) size - Try.of(this::available).getOrElse(0)) / (double) size;
    }

    public double progressPercent() {
        return 100.0 * progress0to1();
    }
}
