package org.sitenetsoft.quarkus.topaz.runtime;

import java.time.Duration;
import java.util.Optional;

/**
 * Effective settings of one Topaz service.
 * Each value comes from the per-service setting, then the root setting, then the default.
 *
 * @param host the host name
 * @param port the port
 * @param apiKey the API key, if any
 * @param tlsConfigurationName the name of the Quarkus TLS configuration, if any
 * @param plainText whether to use plaintext, if set
 * @param deadline the deadline of each call, if any
 */
public record ResolvedServiceSettings(String host, int port, Optional<String> apiKey,
        Optional<String> tlsConfigurationName, Optional<Boolean> plainText, Optional<Duration> deadline) {

    /**
     * Resolve the settings of one service.
     *
     * @param root the root configuration
     * @param service the configuration of the service
     * @param defaultPort the port to use if the configuration sets none
     * @return the effective settings
     */
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
