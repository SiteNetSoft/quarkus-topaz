package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;

import io.quarkus.arc.DefaultBean;
import io.vertx.ext.web.RoutingContext;

/**
 * Resolves no resource method, so the policy path is derived from the raw request path and path parameters are not
 * available. It is a {@code @DefaultBean} used when no REST integration is present: to replace it, provide your own
 * bean implementing {@link ResourceMethodInfoResolver}.
 */
@Singleton
@DefaultBean
public class NoopResourceMethodInfoResolver implements ResourceMethodInfoResolver {

    @Override
    public Optional<ResourceMethodInfo> resolve(RoutingContext ctx) {
        return Optional.empty();
    }
}
