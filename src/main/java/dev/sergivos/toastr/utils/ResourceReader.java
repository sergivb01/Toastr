package dev.sergivos.toastr.utils;

import com.google.common.io.CharStreams;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public final class ResourceReader {
    private ResourceReader() throws IllegalAccessException {
        throw new IllegalAccessException(getClass().getSimpleName() + " cannot be instantiated.");
    }

    /**
     * Read a resource on the jar's classpath into a string.
     * <p>
     * Please note this is potentially a heavy I/O operation which should always be cached.
     *
     * @param path The path on the classpath to read.
     * @return The read string.
     */
    public static @NonNull String readResource(final @NonNull String path) {
        // We use this class as the "entry point" to our jar.
        // All classes should be loaded by the plugin's classloader anyways; at least this very class...
        try(final InputStream stream = ResourceReader.class.getResourceAsStream("/" + path);
            final Reader reader = new InputStreamReader(stream)) {
            return CharStreams.toString(reader);
        } catch(final IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

}
