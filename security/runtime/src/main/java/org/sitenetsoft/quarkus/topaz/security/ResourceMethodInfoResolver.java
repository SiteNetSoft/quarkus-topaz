package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * Resolves the resource method that serves an HTTP request, so mappers can use its path template and path parameters.
 * <p>
 * The default implementation resolves nothing; an implementation backed by Quarkus REST is supplied when it is present.
 */
public interface ResourceMethodInfoResolver {

    /**
     * @param ctx the current request
     * @return the matched resource method, or empty when it cannot be determined
     */
    Optional<ResourceMethodInfo> resolve(RoutingContext ctx);
}
