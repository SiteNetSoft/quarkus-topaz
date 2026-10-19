package org.sitenetsoft.quarkus.topaz.security.norest;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class TodoRoutes {

    @Route(path = "/todos", methods = Route.HttpMethod.GET)
    void list(RoutingContext ctx) {
        ctx.response().end("ok");
    }
}
