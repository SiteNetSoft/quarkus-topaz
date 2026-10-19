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
     * Whether the request body is read and made available to the policy.
     */
    @WithDefault("false")
    boolean readBody();

    /**
     * Permission checks.
     */
    Permissions permissions();

    interface Permissions {
        /**
         * Whether permission-based checks are enabled.
         */
        @WithDefault("false")
        boolean enabled();
    }
}
