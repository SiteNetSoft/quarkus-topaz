package org.sitenetsoft.quarkus.topaz.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Configuration of the Topaz client.
 */
@ConfigMapping(prefix = "quarkus.topaz")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TopazConfig {

    /**
     * API key sent to both services unless overridden per service.
     */
    Optional<String> apiKey();

    /**
     * Name of the Quarkus TLS configuration used for both services unless overridden per service.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Whether to use plaintext (no TLS) connections, unless overridden per service.
     */
    Optional<Boolean> plainText();

    /**
     * Tenant id sent to Topaz.
     */
    Optional<String> tenantId();

    /**
     * Default deadline for calls, unless overridden per service.
     */
    Optional<Duration> deadline();

    /**
     * URL of the Topaz console; set by Dev Services, not read by the client.
     */
    Optional<String> consoleUrl();

    /**
     * Settings of the authorizer service.
     */
    TopazServiceConfig authorizer();

    /**
     * Settings of the directory service.
     */
    TopazServiceConfig directory();
}
