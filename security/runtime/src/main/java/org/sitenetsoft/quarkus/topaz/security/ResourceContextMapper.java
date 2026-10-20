package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

import com.google.protobuf.Struct;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Builds the resource context sent to the Topaz authorizer for an HTTP request.
 * <p>
 * The default contains the path parameters of the matched resource method, then the query parameters (a single value as
 * a string, repeated values as a list); a query parameter never overrides a path parameter of the same name. Provide a
 * CDI bean of this type to replace it.
 * <p>
 * Threading: for the {@code topaz} HTTP security policy, {@code map} runs on a worker thread through the request's
 * Vert.x context, so a blocking body is tolerated, the request's Vert.x duplicated context stays current
 * (Vert.x-context-bound clients and context locals work), and the whole evaluation is bounded by {@code
 * quarkus.topaz.enforcement.timeout} (a mapper that runs out of time yields a denial). The CDI request scope is not
 * active during HTTP security policies, so a mapper must not depend on {@code @RequestScoped} beans. The {@code
 * RoutingContext} is not thread-safe: do not pass it to other threads. When called from {@code @TopazAuthorize} or
 * the permission bridge, {@code map} runs on the calling thread instead, so it should not block there.
 */
public interface ResourceContextMapper {

    /**
     * @param request the current request
     * @param method the matched resource method, when known
     * @return the resource context; a failure denies the request
     */
    Uni<Struct> map(RoutingContext request, Optional<ResourceMethodInfo> method);
}
