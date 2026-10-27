package org.sitenetsoft.quarkus.topaz.security.norest;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * A reactive route with no Quarkus REST, used to test the security extension without it.
 */
@ApplicationScoped
public class TodoRoutes {

    /**
     * Create the routes.
     */
    public TodoRoutes() {
    }

    /**
     * Answer the list request with {@code ok}.
     *
     * @param ctx the routing context
     */
    @Route(path = "/todos", methods = Route.HttpMethod.GET)
    void list(RoutingContext ctx) {
        ctx.response().end("ok");
    }
}
