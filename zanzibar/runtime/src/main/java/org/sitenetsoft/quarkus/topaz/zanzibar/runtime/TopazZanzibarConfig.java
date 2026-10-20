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
     * Subject type used when quarkus-zanzibar hands over a relationship whose user type is blank. The filter does that
     * only on the `quarkus.zanzibar.filter.unauthenticated-user` path, for an endpoint without `@FGAUserType`; the value
     * also applies to a relationship with a blank user type passed to the `RelationshipManager` directly. It does not give
     * an authenticated caller a type: quarkus-zanzibar takes that from the principal name (`type:id`), then a single role,
     * then `@FGAUserType`, and treats the caller as unauthenticated when none of them gives one.
     */
    @WithDefault("user")
    String defaultUserType();
}
