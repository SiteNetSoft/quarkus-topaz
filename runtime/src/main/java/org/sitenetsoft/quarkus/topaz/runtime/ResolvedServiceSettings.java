package org.sitenetsoft.quarkus.topaz.runtime;

import java.time.Duration;
import java.util.Optional;

/**
 * Effective settings of one Topaz service: per-service value, then root value, then default.
 */
public record ResolvedServiceSettings(String host, int port, Optional<String> apiKey,
        Optional<String> tlsConfigurationName, Optional<Boolean> plainText, Optional<Duration> deadline) {

    public static ResolvedServiceSettings resolve(TopazConfig root, TopazServiceConfig service, int defaultPort) {
        return new ResolvedServiceSettings(
                service.host(),
                service.port().orElse(defaultPort),
                service.apiKey().or(root::apiKey),
                service.tlsConfigurationName().or(root::tlsConfigurationName),
                service.plainText().or(root::plainText),
                service.deadline().or(root::deadline));
    }
}
