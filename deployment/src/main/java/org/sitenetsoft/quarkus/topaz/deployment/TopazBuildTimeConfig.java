package org.sitenetsoft.quarkus.topaz.deployment;

import org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazDevServicesConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration of the Topaz extension.
 */
@ConfigMapping(prefix = "quarkus.topaz")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TopazBuildTimeConfig {

    /**
     * Health check settings.
     *
     * @return the health check settings
     */
    HealthConfig health();

    /**
     * Dev Services settings.
     *
     * @return the Dev Services settings
     */
    TopazDevServicesConfig devservices();

    /**
     * Group of the health check settings.
     */
    @ConfigGroup
    interface HealthConfig {

        /**
         * Whether a readiness health check for Topaz is published when a SmallRye Health extension is present.
         *
         * @return whether the readiness health check is published
         */
        @WithDefault("true")
        boolean enabled();
    }
}
