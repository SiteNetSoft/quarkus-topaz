package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.sitenetsoft.quarkus.topaz.Structs;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;
import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;
import org.sitenetsoft.quarkus.topaz.security.TopazResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;

import aserto.authorizer.v2.api.IdentityContext;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * The interceptor that enforces {@link TopazAuthorize}.
 */
@Interceptor
@TopazAuthorize
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)
public class TopazAuthorizeInterceptor {

    private final InterceptorSupport support;
    private final DecisionEvaluator evaluator;
    private final TopazEnforcementConfig config;
    private final IdentityMapper identityMapper;
    private final PolicyPathMapper policyPathMapper;
    private final ResourceContextMapper resourceContextMapper;
    private final ResourceMethodInfoResolver resolver;
    private final ObjectMapper objectMapper;
    private final MapperDispatch dispatch;

    /**
     * Create the interceptor.
     *
     * @param support the shared interceptor plumbing
     * @param evaluator the decision evaluator
     * @param config the enforcement configuration
     * @param identityMapper the identity mapper
     * @param policyPathMapper the policy path mapper
     * @param resourceContextMapper the resource context mapper
     * @param resolver the resource method resolver
     * @param objectMapper the Jackson mapper that converts {@link TopazResource} arguments
     * @param dispatch the mapper dispatcher
     */
    @Inject
    TopazAuthorizeInterceptor(InterceptorSupport support, DecisionEvaluator evaluator, TopazEnforcementConfig config,
            IdentityMapper identityMapper, PolicyPathMapper policyPathMapper,
            ResourceContextMapper resourceContextMapper, ResourceMethodInfoResolver resolver,
            ObjectMapper objectMapper, MapperDispatch dispatch) {
        this.support = support;
        this.evaluator = evaluator;
        this.config = config;
        this.identityMapper = identityMapper;
        this.policyPathMapper = policyPathMapper;
        this.resourceContextMapper = resourceContextMapper;
        this.resolver = resolver;
        this.objectMapper = objectMapper;
        this.dispatch = dispatch;
    }

    /**
     * Check the decision before the intercepted method runs.
     *
     * @param ctx the invocation
     * @return the result of the intercepted method
     * @throws Exception the exception of the intercepted method, or of the check
     */
    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        TopazAuthorize binding = ctx.getInterceptorBinding(TopazAuthorize.class);
        // Read the request on the calling thread, where the request context is active.
        Optional<RoutingContext> request = support.request();
        Uni<SecurityIdentity> identity = support.identity();
        Object[] args = ctx.getParameters();
        Method method = ctx.getMethod();
        // Deny on any failure. failClosed covers the identity lookup, so a failing or hanging identity denies.
        Uni<Void> check = InterceptorSupport.failClosed(identity.chain(id -> {
            if (id.isAnonymous() && config.anonymous() == TopazEnforcementConfig.Anonymous.DENY) {
                return Uni.createFrom().failure(new UnauthorizedException());
            }
            // The inline assembly runs on this thread, so this thread decides whether it may block.
            return dispatch.run(() -> authorize(binding, id, request, method, args), MapperDispatch.Point.AUTHORIZE,
                    InterceptorSupport.blockingDispatch(method));
        }), method.getDeclaringClass().getName() + "#" + method.getName(), config.timeout());
        return InterceptorSupport.dispatch(ctx, check, config.timeout());
    }

    /**
     * Evaluate the decision for one call.
     *
     * @param binding the annotation
     * @param id the caller's identity
     * @param request the current HTTP request, if any
     * @param method the intercepted method
     * @param args the call arguments
     * @return a result that fails with {@link ForbiddenException} when the decision denies
     */
    private Uni<Void> authorize(TopazAuthorize binding, SecurityIdentity id, Optional<RoutingContext> request,
            Method method, Object[] args) {
        String policyPath = binding.path().isEmpty() ? derivePath(request) : binding.path();
        String decision = binding.decision().isEmpty() ? config.decision() : binding.decision();
        List<String> extra = binding.decisions().length > 0 ? Arrays.asList(binding.decisions())
                : config.additionalDecisions().orElse(List.of());
        Uni<IdentityContext> identityContext = Objects.requireNonNull(identityMapper.map(id, request),
                "IdentityMapper returned null");
        Uni<Struct> resource = request
                .map(r -> Objects.requireNonNull(resourceContextMapper.map(r, resolver.resolve(r)),
                        "ResourceContextMapper returned null"))
                .orElseGet(() -> Uni.createFrom().item(Struct.getDefaultInstance()));
        return Uni.combine().all().unis(identityContext, resource).asTuple()
                .chain(t -> evaluator.evaluate(t.getItem1(), policyPath, decision, extra,
                        overlay(t.getItem2(), method, args)))
                .map(outcome -> {
                    if (!outcome.permitted()) {
                        throw new ForbiddenException();
                    }
                    return null;
                });
    }

    /**
     * Derive the policy path from the current HTTP request.
     *
     * @param request the current HTTP request, if any
     * @return the policy path
     */
    private String derivePath(Optional<RoutingContext> request) {
        RoutingContext r = request.orElseThrow(() -> new IllegalStateException(
                "@TopazAuthorize without a path can only be used during an HTTP request"));
        return policyPathMapper.map(r, resolver.resolve(r));
    }

    /**
     * Add the {@link TopazResource} arguments to the resource context.
     *
     * @param resource the resource context from the mapper
     * @param method the intercepted method
     * @param args the call arguments
     * @return the resource context with the arguments added
     */
    private Struct overlay(Struct resource, Method method, Object[] args) {
        Struct.Builder builder = (resource == null ? Struct.getDefaultInstance() : resource).toBuilder();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            TopazResource key = parameters[i].getAnnotation(TopazResource.class);
            if (key != null) {
                builder.putFields(key.value(), Structs.value(objectMapper.convertValue(args[i], Object.class)));
            }
        }
        return builder.build();
    }
}
