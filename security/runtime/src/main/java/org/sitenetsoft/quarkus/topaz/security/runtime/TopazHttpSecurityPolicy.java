package org.sitenetsoft.quarkus.topaz.security.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * The named {@code topaz} HTTP security policy.
 * <p>
 * Select it with {@code quarkus.http.auth.permission.<name>.policy=topaz} or
 * {@code @AuthorizationPolicy(name = "topaz")}.
 * <p>
 * Anonymous callers are denied without a call when {@code quarkus.topaz.enforcement.anonymous=deny}. Otherwise the
 * {@link DecisionEvaluator} evaluates the request. When additional decisions are configured, a permitted identity
 * gets the attribute {@value #DECISIONS_ATTRIBUTE}. Its value is a {@code Map<String, Boolean>} of every returned
 * decision.
 */
@Singleton
public class TopazHttpSecurityPolicy implements HttpSecurityPolicy {

    /** The policy name. */
    public static final String NAME = "topaz";

    /** The identity attribute holding the returned decisions. */
    public static final String DECISIONS_ATTRIBUTE = "topaz.decisions";

    private final DecisionEvaluator evaluator;
    private final TopazEnforcementConfig config;

    /**
     * Create the policy.
     *
     * @param evaluator the decision evaluator
     * @param config the enforcement configuration
     */
    @Inject
    TopazHttpSecurityPolicy(DecisionEvaluator evaluator, TopazEnforcementConfig config) {
        this.evaluator = evaluator;
        this.config = config;
    }

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.onItem().transformToUni(id -> {
            if (id.isAnonymous() && config.anonymous() == TopazEnforcementConfig.Anonymous.DENY) {
                return Uni.createFrom().item(CheckResult.DENY);
            }
            return evaluator.evaluateRequest(id, request).map(outcome -> {
                if (!outcome.permitted()) {
                    return CheckResult.DENY;
                }
                if (config.additionalDecisions().filter(d -> !d.isEmpty()).isPresent()) {
                    return new CheckResult(true, QuarkusSecurityIdentity.builder(id)
                            .addAttribute(DECISIONS_ATTRIBUTE, outcome.decisions()).build());
                }
                return CheckResult.PERMIT;
            });
        });
    }

    @Override
    public String name() {
        return NAME;
    }
}
