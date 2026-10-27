package org.sitenetsoft.quarkus.topaz.zanzibar.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configures the Topaz connector for Quarkus Zanzibar at run time.
 */
@ConfigMapping(prefix = "quarkus.topaz.zanzibar")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TopazZanzibarConfig {

    /**
     * The subject type for a relationship whose user type is blank.
     * <p>
     * quarkus-zanzibar hands over such a relationship on the `quarkus.zanzibar.filter.unauthenticated-user` path,
     * for an endpoint without `@FGAUserType`.
     * The value also applies to a relationship with a blank user type passed to the `RelationshipManager` directly.
     * It does not give an authenticated caller a type.
     * quarkus-zanzibar takes that type from the principal name (`type:id`), then a single role, then `@FGAUserType`.
     * It treats the caller as unauthenticated when none of them gives one.
     *
     * @return the default subject type
     */
    @WithDefault("user")
    String defaultUserType();
}
