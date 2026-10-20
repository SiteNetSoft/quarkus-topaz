package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * Derives the Rego policy path (package) evaluated for an HTTP request.
 * <p>
 * The default follows the convention {@code <policy-root>.<METHOD>.<path>}, where {@code /} separators become
 * {@code .} and template parameters {@code {name}} become {@code __name}; it uses the resource method's path template
 * when known, the request path otherwise. Provide a CDI bean of this type to replace it.
 * <p>
 * Threading: for the {@code topaz} HTTP security policy, {@code map} runs on a worker thread through the request's
 * Vert.x context, so a blocking body is tolerated, the request's Vert.x duplicated context stays current
 * (Vert.x-context-bound clients and context locals work), and the whole evaluation is bounded by {@code
 * quarkus.topaz.enforcement.timeout} (a mapper that runs out of time yields a denial). The CDI request scope is not
 * active during HTTP security policies, so a mapper must not depend on {@code @RequestScoped} beans. The {@code
 * RoutingContext} is not thread-safe: do not pass it to other threads. When called from {@code @TopazAuthorize},
 * {@code map} runs on the calling thread instead, so it should not block there.
 */
public interface PolicyPathMapper {

    /**
     * @param request the current request
     * @param method the matched resource method, when known
     * @return the policy path, e.g. {@code todoApp.GET.todos.__id}
     */
    String map(RoutingContext request, Optional<ResourceMethodInfo> method);
}
