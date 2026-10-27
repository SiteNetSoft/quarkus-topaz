package org.sitenetsoft.quarkus.topaz.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Run-time configuration of the Topaz client.
 */
@ConfigMapping(prefix = "quarkus.topaz")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TopazConfig {

    /**
     * API key sent to both services unless overridden per service.
     *
     * @return the API key, if set
     */
    Optional<String> apiKey();

    /**
     * Name of the Quarkus TLS configuration used for both services unless overridden per service.
     *
     * @return the name of the TLS configuration, if set
     */
    Optional<String> tlsConfigurationName();

    /**
     * Whether to use plaintext (no TLS) connections, unless overridden per service.
     *
     * @return whether to use plaintext, if set
     */
    Optional<Boolean> plainText();

    /**
     * Tenant id sent to Topaz.
     *
     * @return the tenant id, if set
     */
    Optional<String> tenantId();

    /**
     * Default deadline for calls, unless overridden per service.
     *
     * @return the deadline, if set
     */
    Optional<Duration> deadline();

    /**
     * URL of the Topaz console; set by Dev Services, not read by the client.
     *
     * @return the console URL, if set
     */
    Optional<String> consoleUrl();

    /**
     * Settings of the authorizer service.
     *
     * @return the authorizer settings
     */
    TopazServiceConfig authorizer();

    /**
     * Settings of the directory service.
     *
     * @return the directory settings
     */
    TopazServiceConfig directory();
}
