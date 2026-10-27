package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;

import io.quarkus.arc.DefaultBean;
import io.vertx.ext.web.RoutingContext;

/**
 * The default {@link ResourceMethodInfoResolver}, which resolves no resource method.
 * <p>
 * The policy path is then derived from the raw request path, and path parameters are not available.
 * <p>
 * It is a {@code @DefaultBean} used when no REST integration is present. To replace it, provide your own bean
 * implementing {@link ResourceMethodInfoResolver}.
 */
@Singleton
@DefaultBean
public class NoopResourceMethodInfoResolver implements ResourceMethodInfoResolver {

    /**
     * Create the NoopResourceMethodInfoResolver.
     */
    public NoopResourceMethodInfoResolver() {
    }

    @Override
    public Optional<ResourceMethodInfo> resolve(RoutingContext ctx) {
        return Optional.empty();
    }
}
