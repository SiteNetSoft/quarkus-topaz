package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;
import org.sitenetsoft.quarkus.topaz.security.TopazResource;

// The binding sits on the class only: its business methods carry none of their own.
@ApplicationScoped
@TopazAuthorize(path = "todoApp.DELETE.todos.__id")
public class AuthorizeBoundService {

    public String delete(@TopazResource("id") String id) {
        return "deleted " + id;
    }
}
