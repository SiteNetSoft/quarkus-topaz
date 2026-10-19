package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Writes the Topaz {@code config.yaml} for the Dev Service: plaintext gRPC listeners (no {@code certs}), an edge
 * directory, and the policy source and API key from {@link TopazDevServicesConfig}. Built as a map tree and
 * serialised with Jackson YAML so user values (keys, image names) are quoted correctly.
 */
public final class TopazConfigFile {

    static final String CONTAINER_PATH = "/config/config.yaml";
    static final String POLICIES_PATH = "/policies";

    static final int AUTHORIZER_PORT = 8282;
    static final int AUTHORIZER_GATEWAY_PORT = 8383;
    static final int DIRECTORY_PORT = 9292;
    static final int DIRECTORY_GATEWAY_PORT = 9393;
    static final int HEALTH_PORT = 9494;
    static final int CONSOLE_GRPC_PORT = 8081;
    static final int CONSOLE_PORT = 8080;

    /** Paths that stay anonymous when an API key is required, as in Topaz's own default config. */
    static final List<String> ANONYMOUS_PATHS = List.of(
            "/aserto.authorizer.v2.Authorizer/Info",
            "/grpc.reflection.v1.ServerReflection/ServerReflectionInfo",
            "/grpc.reflection.v1alpha.ServerReflection/ServerReflectionInfo");

    private static final List<String> DIRECTORY_SERVICES = List.of("reader", "writer", "model", "importer", "exporter");

    private TopazConfigFile() {
    }

    /** Writes {@code dir/config.yaml} and returns its path. */
    public static Path write(Path dir, TopazDevServicesConfig cfg) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve("config.yaml");
            Files.writeString(file, render(cfg));
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the Topaz Dev Services config to " + dir, e);
        }
    }

    static String render(TopazDevServicesConfig cfg) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 2);
        root.put("logging", map("prod", true, "log_level", "info"));
        root.put("directory", map("db_path", "/db/topaz.db"));

        Map<String, Object> remote = map("address", "0.0.0.0:" + DIRECTORY_PORT,
                // insecure defaults to true and Topaz rejects insecure together with no_tls
                "insecure", false, "no_tls", true);
        cfg.apiKey().ifPresent(k -> remote.put("api_key", k));
        root.put("remote_directory", remote);

        boolean withKey = cfg.apiKey().isPresent();
        Map<String, Object> auth = new LinkedHashMap<>();
        cfg.apiKey().ifPresent(k -> auth.put("keys", List.of(k)));
        auth.put("options", map(
                "default", map("enable_api_key", withKey, "enable_anonymous", !withKey),
                "overrides", map("paths", ANONYMOUS_PATHS,
                        "override", map("enable_api_key", false, "enable_anonymous", true))));
        root.put("auth", auth);

        Map<String, Object> services = new LinkedHashMap<>();
        for (String s : DIRECTORY_SERVICES) {
            services.put(s, service(DIRECTORY_PORT, DIRECTORY_GATEWAY_PORT));
        }
        services.put("authorizer", service(AUTHORIZER_PORT, AUTHORIZER_GATEWAY_PORT));
        if (cfg.consoleEnabled()) {
            services.put("console", service(CONSOLE_GRPC_PORT, CONSOLE_PORT));
        }
        root.put("api", map("health", map("listen_address", "0.0.0.0:" + HEALTH_PORT), "services", services));

        Map<String, Object> opa = map("instance_id", "-", "graceful_shutdown_period_seconds", 2);
        cfg.policy().directory().ifPresent(d -> opa.put("local_bundles",
                map("paths", List.of(POLICIES_PATH), "skip_verification", true)));
        cfg.policy().image().ifPresent(image -> opa.put("config", map(
                "services", map("ghcr", map("url", "https://" + registryHost(image), "type", "oci",
                        "response_header_timeout_seconds", 15)),
                "bundles", map("policy", map("service", "ghcr", "resource", image, "persist", false,
                        "config", map("polling", map("min_delay_seconds", 60, "max_delay_seconds", 120)))))));
        root.put("opa", opa);

        try {
            return new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                    .writeValueAsString(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The registry of an OCI reference: its first segment when that looks like a host, else Docker Hub. */
    static String registryHost(String image) {
        int slash = image.indexOf('/');
        if (slash > 0) {
            String first = image.substring(0, slash);
            if (first.contains(".") || first.contains(":") || first.equals("localhost")) {
                return first;
            }
        }
        return "registry-1.docker.io";
    }

    private static Map<String, Object> service(int grpcPort, int gatewayPort) {
        return map("grpc", map("listen_address", "0.0.0.0:" + grpcPort),
                "gateway", map("listen_address", "0.0.0.0:" + gatewayPort, "http", true));
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
