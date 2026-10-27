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
 * It is effective only once REST matching has happened, that is for policies applied with {@code applies-to=jaxrs} or
 * {@code @AuthorizationPolicy}. Before matching, and on any failure, it returns {@link Optional#empty()}. It never
 * throws.
 * <p>
 * It is registered only when the REST capability is present. Without quarkus-rest, the deployment processor excludes
 * this class from bean discovery. The runtime jar is indexed, so bean discovery would find the class otherwise.
 */
@Singleton
public class RestResourceMethodInfoResolver implements ResourceMethodInfoResolver {

    private static final Logger LOG = Logger.getLogger(RestResourceMethodInfoResolver.class);

    /**
     * Create the RestResourceMethodInfoResolver.
     */
    public RestResourceMethodInfoResolver() {
    }

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
            // Fail closed: the evaluator then maps the raw path, which has no policy.
            // LinkageError covers a classpath without quarkus-rest, should this class ever be registered there.
            LOG.debug("Could not resolve the REST resource method", e);
            return Optional.empty();
        }
    }

    /**
     * Get the text of a path template.
     *
     * @param template the template, possibly null
     * @return the template text, or empty for a null template
     */
    private static String template(URITemplate template) {
        return template == null ? "" : template.template;
    }

    /**
     * Join a class path template and a method path template into one path.
     * <p>
     * The path starts with {@code /} and has no double or trailing slashes. The root stays {@code /}.
     *
     * @param classPath the class path template
     * @param methodPath the method path template
     * @return the joined path
     */
    static String joinTemplates(String classPath, String methodPath) {
        String joined = ("/" + classPath + "/" + methodPath).replaceAll("/{2,}", "/");
        return joined.length() > 1 && joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }
}
