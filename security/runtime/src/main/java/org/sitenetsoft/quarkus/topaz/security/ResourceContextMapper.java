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
 */
public interface ResourceContextMapper {

    /**
     * @param request the current request
     * @param method the matched resource method, when known
     * @return the resource context; a failure denies the request
     */
    Uni<Struct> map(RoutingContext request, Optional<ResourceMethodInfo> method);
}
