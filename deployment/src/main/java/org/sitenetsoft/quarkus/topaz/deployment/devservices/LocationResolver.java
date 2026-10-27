package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Resolver of {@code quarkus.topaz.devservices.*} locations to local paths.
 * It copies {@code classpath:} resources under the output directory, so that they can be copied into the container.
 * The resources are files or directories, also inside jars.
 * It uses {@code filesystem:} and bare locations as they are.
 */
final class LocationResolver {

    /** Prefix of a classpath location. */
    static final String CLASSPATH = "classpath:";
    /** Prefix of a file system location. */
    static final String FILESYSTEM = "filesystem:";

    private final Path outputDir;

    /**
     * Create a resolver.
     *
     * @param outputDir the directory that receives the copies of classpath resources
     */
    LocationResolver(Path outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Resolve a location to a local path.
     *
     * @param key the config key below {@code quarkus.topaz.devservices.}, for the error message
     * @param location the configured location
     * @param name the name of the copy under the output directory, for classpath locations only
     * @return the local path
     */
    Path resolve(String key, String location, String name) {
        if (location.startsWith(CLASSPATH)) {
            String resource = location.substring(CLASSPATH.length());
            while (resource.startsWith("/")) {
                resource = resource.substring(1);
            }
            URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
            if (url == null) {
                throw missing(key, location);
            }
            return copy(url, outputDir.resolve(name));
        }
        String path = location.startsWith(FILESYSTEM) ? location.substring(FILESYSTEM.length()) : location;
        Path p = Path.of(path).toAbsolutePath();
        if (!Files.exists(p)) {
            throw missing(key, location);
        }
        return p;
    }

    private static ConfigurationException missing(String key, String location) {
        return new ConfigurationException(
                "quarkus.topaz.devservices." + key + " points to a missing location: " + location);
    }

    private static Path copy(URL url, Path target) {
        try {
            URI uri = url.toURI();
            deleteRecursively(target);
            if ("jar".equals(uri.getScheme())) {
                FileSystem fs;
                boolean owned;
                try {
                    fs = FileSystems.newFileSystem(uri, Map.of());
                    owned = true;
                } catch (FileSystemAlreadyExistsException e) {
                    fs = FileSystems.getFileSystem(uri);
                    owned = false;
                }
                try {
                    String entry = uri.getSchemeSpecificPart();
                    copyTree(fs.getPath(entry.substring(entry.indexOf("!/") + 1)), target);
                } finally {
                    if (owned) {
                        fs.close();
                    }
                }
            } else {
                copyTree(Path.of(uri), target);
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot copy " + url + " to " + target, e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        if (!Files.isDirectory(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                Path dest = target.resolve(source.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursively(Path p) throws IOException {
        if (!Files.exists(p)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(p)) {
            for (Path x : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(x);
            }
        }
    }
}
