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
     * Get the policy root, which is the Rego package prefix that decisions are evaluated under.
     *
     * @return the policy root, if set
     */
    Optional<String> policyRoot();

    /**
     * Get the name of the decision that must be true for a request to be allowed.
     *
     * @return the decision name
     */
    @WithDefault("allowed")
    String decision();

    /**
     * Get the further decisions to request from the authorizer alongside the main one.
     *
     * @return the further decisions, if set
     */
    Optional<List<String>> additionalDecisions();

    /**
     * Get what to do with requests that carry no identity.
     *
     * @return the handling of anonymous requests
     */
    @WithDefault("deny")
    Anonymous anonymous();

    /**
     * Get the deadline for a whole evaluation.
     * <p>
     * The deadline covers the mappers (identity, policy path and resource context) and the Topaz call together. It does
     * not apply to each call separately. An evaluation that exceeds it is denied.
     *
     * @return the deadline
     */
    @WithDefault("5s")
    Duration timeout();

    /**
     * The handling of requests that carry no identity.
     */
    enum Anonymous {
        /** Reject anonymous requests without calling the authorizer. */
        DENY,
        /** Evaluate anonymous requests against the policy. */
        EVALUATE
    }
}
