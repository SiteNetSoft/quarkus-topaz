package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;
import org.sitenetsoft.quarkus.topaz.security.TopazResource;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class SecuredService {

    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public Uni<String> deleteReactive(@TopazResource("id") String id) {
        return Uni.createFrom().item("deleted " + id);
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public String deleteBlocking(@TopazResource("id") String id) {
        return "deleted " + id;
    }

    @TopazAuthorize(path = "todoApp.GET.todos", decision = "allowed")
    public String list() {
        return "ok";
    }
}
