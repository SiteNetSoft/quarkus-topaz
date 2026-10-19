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
     */
    @WithDefault("localhost")
    String host();

    /**
     * Port of the service; defaults to 8282 for the authorizer and 9292 for the directory.
     */
    OptionalInt port();

    /**
     * API key for this service.
     */
    Optional<String> apiKey();

    /**
     * Name of the Quarkus TLS configuration for this service.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Whether to use a plaintext (no TLS) connection to this service.
     */
    Optional<Boolean> plainText();

    /**
     * Deadline for calls to this service.
     */
    Optional<Duration> deadline();
}
