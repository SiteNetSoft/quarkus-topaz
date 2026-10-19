package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@Path("/service")
@Produces(MediaType.TEXT_PLAIN)
public class ServiceResource {

    @Inject
    SecuredService service;

    @GET
    @Path("list")
    public String list() {
        return service.list();
    }

    @DELETE
    @Path("reactive/{id}")
    public Uni<String> deleteReactive(@PathParam("id") String id) {
        return service.deleteReactive(id);
    }

    @DELETE
    @Path("blocking/{id}")
    @Blocking
    public String deleteBlocking(@PathParam("id") String id) {
        return service.deleteBlocking(id);
    }
}
