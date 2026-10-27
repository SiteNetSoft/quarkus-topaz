package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * Derives the Rego policy path, which is the package, evaluated for an HTTP request.
 * <p>
 * The default follows the convention {@code <policy-root>.<METHOD>.<path>}. Each {@code /} separator becomes
 * {@code .} and each template parameter {@code {name}} becomes {@code __name}. The default uses the resource method's
 * path template when known and the request path otherwise. Provide a CDI bean of this type to replace it.
 * <p>
 * Threading: {@code map} runs on a thread that may block, unless the mapper returns {@code false} from
 * {@link #blocking()}. The request's Vert.x duplicated context stays current. The {@code
 * quarkus.topaz.enforcement.timeout} bounds the whole evaluation. A mapper that runs out of time yields a denial.
 * A mapper must not use {@code @RequestScoped} beans. The {@code RoutingContext} is not thread-safe, so do not pass it
 * to other threads.
 */
public interface PolicyPathMapper {

    /**
     * Derive the policy path for a request.
     *
     * @param request the current request
     * @param method the matched resource method, when known
     * @return the policy path, e.g. {@code todoApp.GET.todos.__id}
     */
    String map(RoutingContext request, Optional<ResourceMethodInfo> method);

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
