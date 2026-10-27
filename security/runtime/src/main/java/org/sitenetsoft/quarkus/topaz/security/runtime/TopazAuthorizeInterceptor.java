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
 * Enforces {@link TopazAuthorize}.
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

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        TopazAuthorize binding = ctx.getInterceptorBinding(TopazAuthorize.class);
        // read on the calling thread, where the request context is active
        Optional<RoutingContext> request = support.request();
        Uni<SecurityIdentity> identity = support.identity();
        Object[] args = ctx.getParameters();
        Method method = ctx.getMethod();
        boolean callerMayBlock = InterceptorSupport.blockingDispatch(method);
        // failClosed covers the identity lookup too: a failing or hanging identity denies within the timeout
        Uni<Void> check = InterceptorSupport.failClosed(identity.chain(id -> {
            if (id.isAnonymous() && config.anonymous() == TopazEnforcementConfig.Anonymous.DENY) {
                return Uni.createFrom().failure(new UnauthorizedException());
            }
            return dispatch.run(() -> authorize(binding, id, request, method, args), MapperDispatch.Point.AUTHORIZE,
                    callerMayBlock);
        }), method.getDeclaringClass().getName() + "#" + method.getName(), config.timeout());
        return InterceptorSupport.dispatch(ctx, check, config.timeout());
    }

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

    private String derivePath(Optional<RoutingContext> request) {
        RoutingContext r = request.orElseThrow(() -> new IllegalStateException(
                "@TopazAuthorize without a path can only be used during an HTTP request"));
        return policyPathMapper.map(r, resolver.resolve(r));
    }

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
