package org.sitenetsoft.quarkus.topaz.security.runtime.rest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;

import io.vertx.ext.web.RoutingContext;

/**
 * Resolves the Quarkus REST resource method matched for the current request.
 * <p>
 * Only effective once REST matching has happened, i.e. for policies applied with {@code applies-to=jaxrs} or
 * {@code @AuthorizationPolicy}. Before matching, and on any failure, it returns {@link Optional#empty()}; it never
 * throws. Registered only when the REST capability is present; without quarkus-rest the deployment processor excludes
 * this class from bean discovery (the runtime jar is indexed, so it would otherwise be discovered).
 */
@Singleton
public class RestResourceMethodInfoResolver implements ResourceMethodInfoResolver {

    private static final Logger LOG = Logger.getLogger(RestResourceMethodInfoResolver.class);

    @Override
    public Optional<ResourceMethodInfo> resolve(RoutingContext request) {
        try {
            ResteasyReactiveRequestContext ctx = CurrentRequestManager.get();
            if (ctx == null) {
                return Optional.empty();
            }
            RuntimeResource target = ctx.getTarget();
            if (target == null) {
                return Optional.empty();
            }
            String httpMethod = target.getHttpMethod() != null ? target.getHttpMethod()
                    : request.request().method().name();
            Map<String, String> params = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> e : target.getPathParameterIndexes().entrySet()) {
                String value = ctx.getPathParam(e.getValue(), false);
                if (value != null) {
                    params.put(e.getKey(), value);
                }
            }
            return Optional.of(new ResourceMethodInfo(httpMethod,
                    joinTemplates(template(target.getClassPath()), template(target.getPath())), params));
        } catch (RuntimeException | LinkageError e) {
            // Fail closed: the evaluator then maps the raw path, which has no policy. LinkageError covers a
            // classpath without quarkus-rest, should this class ever be registered there.
            LOG.debug("Could not resolve the REST resource method", e);
            return Optional.empty();
        }
    }

    private static String template(URITemplate template) {
        return template == null ? "" : template.template;
    }

    /**
     * Joins a class path template and a method path template into one path starting with {@code /}, without double
     * or trailing slashes (the root stays {@code /}).
     */
    static String joinTemplates(String classPath, String methodPath) {
        String joined = ("/" + classPath + "/" + methodPath).replaceAll("/{2,}", "/");
        return joined.length() > 1 && joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }
}
