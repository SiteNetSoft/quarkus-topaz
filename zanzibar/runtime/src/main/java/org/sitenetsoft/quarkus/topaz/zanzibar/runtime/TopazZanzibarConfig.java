package org.sitenetsoft.quarkus.topaz.zanzibar.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Run-time configuration of the Topaz connector for Quarkus Zanzibar.
 */
@ConfigMapping(prefix = "quarkus.topaz.zanzibar")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TopazZanzibarConfig {

    /**
     * Subject type used when quarkus-zanzibar hands over a relationship whose user type is blank (no `@FGAUserType` on the endpoint).
     */
    @WithDefault("user")
    String defaultUserType();
}
