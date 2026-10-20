package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vertx.web.Route;

@ApplicationScoped
public class EventLoopRoutes {

    @Inject
    SecuredService service;

    // a non-blocking route runs on the event loop and calls a blocking @TopazAuthorize method there
    @Route(path = "/loop/blocking", methods = Route.HttpMethod.GET)
    String blockingOnEventLoop() {
        return service.deleteBlocking("todo-1");
    }
}
