package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;

@ApplicationScoped
public class NotAResource {

    @TopazAuthorize
    public String secured() {
        return "secured";
    }
}
