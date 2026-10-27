package org.sitenetsoft.quarkus.topaz.security.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration of Topaz policy enforcement.
 */
@ConfigMapping(prefix = "quarkus.topaz.enforcement")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TopazEnforcementBuildTimeConfig {

    /**
     * Get whether the request body is read and made available to the policy.
     *
     * @return {@code true} when the request body is read
     */
    @WithDefault("false")
    boolean readBody();

    /**
     * Get the permission check configuration.
     *
     * @return the permission check configuration
     */
    Permissions permissions();

    /**
     * The configuration of permission-based checks.
     */
    interface Permissions {
        /**
         * Get whether permission-based checks are enabled.
         *
         * @return {@code true} when permission-based checks are enabled
         */
        @WithDefault("false")
        boolean enabled();
    }
}
