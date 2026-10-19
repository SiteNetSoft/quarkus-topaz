package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.topaz.AuthorizerClient;
import org.sitenetsoft.quarkus.topaz.security.DecisionCache;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;

import aserto.authorizer.v2.Decision;
import aserto.authorizer.v2.api.IdentityContext;
import aserto.authorizer.v2.api.IdentityType;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Evaluates Topaz decisions for a request. Every enforcement point delegates here.
 * <p>
 * Fails closed: an authorizer error, a timeout, a mapper failure or a response without the configured decision yields
 * a denying {@link Outcome} and a WARN log naming the policy path. Successful decisions go through the
 * {@link DecisionCache}; errors are never cached.
 */
@Singleton
public final class DecisionEvaluator {

    private static final Logger LOG = Logger.getLogger(DecisionEvaluator.class);
    private static final JsonFormat.Printer JSON = JsonFormat.printer().omittingInsignificantWhitespace();

    /**
     * The result of an evaluation.
     *
     * @param permitted whether the configured decision is true
     * @param decisions every decision the authorizer returned, empty on a failure
     */
    public record Outcome(boolean permitted, Map<String, Boolean> decisions) {

        static final Outcome DENIED = new Outcome(false, Map.of());
    }

    private final AuthorizerClient authorizer;
    private final TopazEnforcementConfig config;
    private final DecisionCache cache;
    private final IdentityMapper identityMapper;
    private final PolicyPathMapper policyPathMapper;
    private final ResourceContextMapper resourceContextMapper;
    private final ResourceMethodInfoResolver resolver;

    @Inject
    DecisionEvaluator(AuthorizerClient authorizer, TopazEnforcementConfig config, DecisionCache cache,
            IdentityMapper identityMapper, PolicyPathMapper policyPathMapper,
            ResourceContextMapper resourceContextMapper, ResourceMethodInfoResolver resolver) {
        this.authorizer = authorizer;
        this.config = config;
        this.cache = cache;
        this.identityMapper = identityMapper;
        this.policyPathMapper = policyPathMapper;
        this.resourceContextMapper = resourceContextMapper;
        this.resolver = resolver;
    }

    /**
     * Evaluates the configured decision and the additional decisions for an HTTP request, mapping the identity, the
     * policy path and the resource context with the configured mappers.
     *
     * @param identity the caller
     * @param request the HTTP request
     * @return the outcome; never a failure
     */
    public Uni<Outcome> evaluateRequest(SecurityIdentity identity, RoutingContext request) {
        Optional<ResourceMethodInfo> method;
        String policyPath;
        try {
            method = resolver.resolve(request);
            policyPath = policyPathMapper.map(request, method);
        } catch (RuntimeException e) {
            return Uni.createFrom().item(deny("<unresolved policy path for " + request.request().method().name()
                    + " " + request.normalizedPath() + ">", describe(identity), e));
        }
        Uni<Outcome> outcome;
        try {
            Uni<IdentityContext> id = Objects.requireNonNull(identityMapper.map(identity, Optional.of(request)),
                    "IdentityMapper returned null");
            Uni<Struct> resource = Objects.requireNonNull(resourceContextMapper.map(request, method),
                    "ResourceContextMapper returned null");
            outcome = Uni.combine().all().unis(id, resource).asTuple()
                    .onItem().transformToUni(t -> evaluate(t.getItem1(), policyPath, t.getItem2()));
        } catch (RuntimeException e) {
            return Uni.createFrom().item(deny(policyPath, describe(identity), e));
        }
        // The timeout covers the mappers too, so a mapper that never emits cannot hang the request.
        return outcome
                .ifNoItem().after(config.timeout()).fail()
                .onFailure().recoverWithItem(e -> deny(policyPath, describe(identity), e));
    }

    /**
     * Evaluates the configured decision and the additional decisions.
     *
     * @param identity the identity context sent to the authorizer
     * @param policyPath the policy path
     * @param resource the resource context, may be {@code null}
     * @return the outcome; never a failure
     */
    public Uni<Outcome> evaluate(IdentityContext identity, String policyPath, Struct resource) {
        return evaluate(identity, policyPath, config.decision(), config.additionalDecisions().orElse(List.of()),
                resource);
    }

    /**
     * Evaluates the given decision and extra decisions.
     *
     * @param identity the identity context sent to the authorizer
     * @param policyPath the policy path
     * @param decision the decision that must be true for {@link Outcome#permitted()}
     * @param extraDecisions further decisions to request, may be empty
     * @param resource the resource context, may be {@code null}
     * @return the outcome; never a failure
     */
    public Uni<Outcome> evaluate(IdentityContext identity, String policyPath, String decision,
            List<String> extraDecisions, Struct resource) {
        List<String> decisions;
        Map<String, String> keys = new LinkedHashMap<>();
        Map<String, Boolean> cached = new LinkedHashMap<>();
        try {
            decisions = decisions(Objects.requireNonNull(decision, "decision"), extraDecisions);
            String resourceJson = JSON.print(resource == null ? Struct.getDefaultInstance() : resource);
            for (String d : decisions) {
                String key = cacheKey(identity, policyPath, d, resourceJson);
                keys.put(d, key);
                cache.get(key).ifPresent(v -> cached.put(d, v));
            }
        } catch (InvalidProtocolBufferException | RuntimeException e) {
            return Uni.createFrom().item(deny(policyPath, describe(identity), e));
        }
        if (cached.size() == decisions.size()) {
            return Uni.createFrom().item(new Outcome(cached.get(decision), Map.copyOf(cached)));
        }
        // deferred: the client builds the request synchronously, and evaluate must never throw
        return Uni.createFrom().deferred(() -> authorizer.is(identity, policyPath, decisions, resource))
                .ifNoItem().after(config.timeout()).fail()
                .map(response -> {
                    Map<String, Boolean> returned = new LinkedHashMap<>();
                    for (Decision d : response.getDecisionsList()) {
                        returned.put(d.getDecision(), d.getIs());
                    }
                    Boolean permitted = returned.get(decision);
                    if (permitted == null) {
                        throw new IllegalStateException("response lacks decision '" + decision + "'");
                    }
                    returned.forEach((name, value) -> {
                        String key = keys.get(name);
                        if (key != null) {
                            cache.put(key, value);
                        }
                    });
                    return new Outcome(permitted, Map.copyOf(returned));
                })
                .onFailure().recoverWithItem(e -> deny(policyPath, describe(identity), e));
    }

    // The identity may be a raw JWT: the key is a digest so that a cache never holds a credential in clear.
    static String cacheKey(IdentityContext identity, String policyPath, String decision, String resourceJson) {
        String material = identity.getType() + "|" + identity.getIdentity() + "|" + policyPath + "|" + decision + "|"
                + resourceJson;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static List<String> decisions(String decision, List<String> extraDecisions) {
        Set<String> decisions = new LinkedHashSet<>();
        decisions.add(decision);
        if (extraDecisions != null) {
            decisions.addAll(extraDecisions);
        }
        return new ArrayList<>(decisions);
    }

    private static Outcome deny(String policyPath, String identity, Throwable cause) {
        LOG.warnf("Topaz denied %s for %s: %s", policyPath, identity, cause.toString());
        return Outcome.DENIED;
    }

    // A JWT is a credential: log only its type.
    private static String describe(IdentityContext identity) {
        if (identity == null) {
            return "<no identity>";
        }
        if (identity.getType() == IdentityType.IDENTITY_TYPE_JWT) {
            return identity.getType().name();
        }
        return identity.getType().name() + " " + identity.getIdentity();
    }

    private static String describe(SecurityIdentity identity) {
        return identity.isAnonymous() ? "anonymous" : identity.getPrincipal().getName();
    }
}
