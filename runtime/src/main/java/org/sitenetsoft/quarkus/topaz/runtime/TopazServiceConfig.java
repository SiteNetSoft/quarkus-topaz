package org.sitenetsoft.quarkus.topaz.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Per-service connection settings; each value overrides the corresponding root value.
 */
@ConfigGroup
public interface TopazServiceConfig {

    /**
     * Host of the service.
     *
     * @return the host name
     */
    @WithDefault("localhost")
    String host();

    /**
     * Port of the service.
     * It defaults to 8282 for the authorizer and 9292 for the directory.
     *
     * @return the port, if set
     */
    OptionalInt port();

    /**
     * API key for this service.
     *
     * @return the API key, if set
     */
    Optional<String> apiKey();

    /**
     * Name of the Quarkus TLS configuration for this service.
     *
     * @return the name of the TLS configuration, if set
     */
    Optional<String> tlsConfigurationName();

    /**
     * Whether to use a plaintext (no TLS) connection to this service.
     *
     * @return whether to use plaintext, if set
     */
    Optional<Boolean> plainText();

    /**
     * Deadline for calls to this service.
     *
     * @return the deadline, if set
     */
    Optional<Duration> deadline();
}
