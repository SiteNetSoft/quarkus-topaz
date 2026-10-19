package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.topaz.ImportSummary;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import aserto.directory.common.v3.Object;
import aserto.directory.common.v3.Relation;
import aserto.directory.importer.v3.ImportRequest;
import aserto.directory.importer.v3.Opcode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Multi;

/**
 * Loads the manifest and data into a Topaz Dev Service, or (for a shared container someone else started) only
 * compares manifests.
 */
final class TopazSeeder {

    private static final Logger LOG = Logger.getLogger(TopazSeeder.class);

    static final String OBJECTS_FILE = "objects.json";
    static final String RELATIONS_FILE = "relations.json";

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    /** GetManifest right after SetManifest can briefly return only the metadata (Task 3 spike). */
    private static final Duration MANIFEST_VISIBLE_TIMEOUT = Duration.ofSeconds(5);

    private TopazSeeder() {
    }

    /**
     * @param data the requests from {@link #requests(Path)}, parsed at build time so bad data fails the build
     * @param dataDir where they came from, for messages
     */
    static void seed(TopazClient client, Optional<Path> manifest, Optional<List<ImportRequest>> data,
            Optional<Path> dataDir) {
        if (manifest.isPresent()) {
            String yaml = read(manifest.get());
            client.directory().setManifest(yaml).await().atMost(CALL_TIMEOUT);
            awaitManifest(client);
            LOG.debugf("Topaz Dev Services: manifest %s loaded", manifest.get());
        }
        if (data.isPresent()) {
            ImportSummary summary = client.directory().importData(Multi.createFrom().iterable(data.get()))
                    .await().atMost(CALL_TIMEOUT);
            if (!summary.errors().isEmpty()) {
                throw new IllegalStateException("Topaz Dev Services: importing " + dataDir.orElse(null) + " failed: "
                        + summary.errors());
            }
            LOG.infof("Topaz Dev Services: imported %d objects and %d relations from %s", summary.objectsSet(),
                    summary.relationsSet(), dataDir.orElse(null));
        }
    }

    /** For a container this application did not start: warn when its manifest differs, never overwrite it. */
    static void checkDrift(TopazClient client, Path manifest) {
        String expected = read(manifest);
        String actual;
        try {
            actual = client.directory().getManifest().await().atMost(CALL_TIMEOUT);
        } catch (RuntimeException e) {
            LOG.warnf("Topaz Dev Services: could not read the manifest of the shared container (%s); %s is not loaded",
                    e.toString(), manifest);
            return;
        }
        if (!expected.strip().equals(actual.strip())) {
            LOG.warnf("Topaz Dev Services: the shared container's manifest differs from %s; it is not overwritten."
                    + " Stop the shared container or set quarkus.topaz.devservices.shared=false to load yours.",
                    manifest);
        }
    }

    private static void awaitManifest(TopazClient client) {
        long deadline = System.nanoTime() + MANIFEST_VISIBLE_TIMEOUT.toNanos();
        while (client.directory().getManifest().await().atMost(CALL_TIMEOUT).isEmpty()) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException(
                        "Topaz Dev Services: the manifest was not readable back within " + MANIFEST_VISIBLE_TIMEOUT);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    /** Parses the data files strictly: unknown fields are errors. */
    static List<ImportRequest> requests(Path dataDir) {
        Path objects = dataDir.resolve(OBJECTS_FILE);
        Path relations = dataDir.resolve(RELATIONS_FILE);
        if (!Files.isRegularFile(objects) && !Files.isRegularFile(relations)) {
            throw new ConfigurationException("Topaz Dev Services: " + dataDir + " has neither " + OBJECTS_FILE
                    + " nor " + RELATIONS_FILE);
        }
        List<ImportRequest> requests = new ArrayList<>();
        if (Files.isRegularFile(objects)) {
            for (Object o : parse(objects, "objects", Object::newBuilder, Object.Builder::build)) {
                requests.add(ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET).setObject(o).build());
            }
        }
        if (Files.isRegularFile(relations)) {
            for (Relation r : parse(relations, "relations", Relation::newBuilder, Relation.Builder::build)) {
                requests.add(ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET).setRelation(r).build());
            }
        }
        return requests;
    }

    private static <B extends Message.Builder, T> List<T> parse(Path file, String field,
            java.util.function.Supplier<B> builder, Function<B, T> build) {
        JsonNode array;
        try {
            array = new ObjectMapper().readTree(file.toFile()).path(field);
        } catch (IOException e) {
            throw new ConfigurationException("Topaz Dev Services: cannot read " + file + ": " + e.getMessage(), e);
        }
        if (!array.isArray()) {
            throw new ConfigurationException("Topaz Dev Services: " + file + " must be {\"" + field + "\": [...]}");
        }
        List<T> out = new ArrayList<>();
        JsonFormat.Parser parser = JsonFormat.parser();
        for (JsonNode element : array) {
            B b = builder.get();
            try {
                parser.merge(element.toString(), b);
            } catch (InvalidProtocolBufferException e) {
                throw new ConfigurationException("Topaz Dev Services: invalid entry in " + file + ": " + element
                        + " (" + e.getMessage() + ")", e);
            }
            out.add(build.apply(b));
        }
        return out;
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Topaz Dev Services: cannot read " + file, e);
        }
    }
}
