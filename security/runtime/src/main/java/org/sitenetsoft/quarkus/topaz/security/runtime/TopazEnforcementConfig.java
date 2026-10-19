package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Run-time configuration of Topaz policy enforcement.
 */
@ConfigMapping(prefix = "quarkus.topaz.enforcement")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TopazEnforcementConfig {

    /**
     * The policy root (Rego package prefix) that decisions are evaluated under.
     */
    Optional<String> policyRoot();

    /**
     * The name of the decision that must be true for a request to be allowed.
     */
    @WithDefault("allowed")
    String decision();

    /**
     * Further decisions to request from the authorizer alongside the main one.
     */
    Optional<List<String>> additionalDecisions();

    /**
     * What to do with requests that carry no identity.
     */
    @WithDefault("deny")
    Anonymous anonymous();

    /**
     * The deadline for a single authorization call.
     */
    @WithDefault("5s")
    Duration timeout();

    enum Anonymous {
        /** Reject anonymous requests without calling the authorizer. */
        DENY,
        /** Evaluate anonymous requests against the policy. */
        EVALUATE
    }
}
