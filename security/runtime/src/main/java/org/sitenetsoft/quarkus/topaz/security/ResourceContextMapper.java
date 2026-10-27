package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

import com.google.protobuf.Struct;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Builds the resource context sent to the Topaz authorizer for an HTTP request.
 * <p>
 * The default contains the path parameters of the matched resource method, then the query parameters. A single query
 * value is a string and repeated values are a list. A query parameter never overrides a path parameter of the same
 * name. Provide a CDI bean of this type to replace it.
 * <p>
 * Threading: {@code map} runs on a thread that may block, unless the mapper returns {@code false} from
 * {@link #blocking()}. The request's Vert.x duplicated context stays current. The {@code
 * quarkus.topaz.enforcement.timeout} bounds the whole evaluation. A mapper that runs out of time yields a denial.
 * A mapper must not use {@code @RequestScoped} beans. The {@code RoutingContext} is not thread-safe, so do not pass it
 * to other threads.
 */
public interface ResourceContextMapper {

    /**
     * Build the resource context for a request.
     *
     * @param request the current request
     * @param method the matched resource method, when known
     * @return the resource context, where a failure denies the request
     */
    Uni<Struct> map(RoutingContext request, Optional<ResourceMethodInfo> method);

    /**
     * Tell whether {@code map} may block.
     * <p>
     * A mapper that returns {@code false} must not block and must not use {@code @RequestScoped} beans. When no
     * component of an enforcement point blocks, the mappers run on the calling thread. That thread can be the event
     * loop. The default returns {@code true}, so {@code map} runs on a thread that may block.
     *
     * @return {@code true} when {@code map} may block, {@code false} when it never blocks
     */
    default boolean blocking() {
        return true;
    }
}
