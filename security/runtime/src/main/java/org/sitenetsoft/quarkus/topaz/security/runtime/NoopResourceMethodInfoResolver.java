package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;

import io.quarkus.arc.DefaultBean;
import io.vertx.ext.web.RoutingContext;

@Singleton
@DefaultBean
public class NoopResourceMethodInfoResolver implements ResourceMethodInfoResolver {

    @Override
    public Optional<ResourceMethodInfo> resolve(RoutingContext ctx) {
        return Optional.empty();
    }
}
