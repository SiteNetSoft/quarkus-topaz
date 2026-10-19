package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * Derives the Rego policy path (package) evaluated for an HTTP request.
 * <p>
 * The default follows the convention {@code <policy-root>.<METHOD>.<path>}, where {@code /} separators become
 * {@code .} and template parameters {@code {name}} become {@code __name}; it uses the resource method's path template
 * when known, the request path otherwise. Provide a CDI bean of this type to replace it.
 */
public interface PolicyPathMapper {

    /**
     * @param request the current request
     * @param method the matched resource method, when known
     * @return the policy path, e.g. {@code todoApp.GET.todos.__id}
     */
    String map(RoutingContext request, Optional<ResourceMethodInfo> method);
}
