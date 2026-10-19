package org.sitenetsoft.quarkus.topaz.security.deployment;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.identity.SecurityIdentity;

@Path("/todos")
public class TodoTestResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Boolean> list() {
        Map<String, Boolean> decisions = identity.getAttribute("topaz.decisions");
        return decisions == null ? Map.of() : decisions;
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) {
    }
}
