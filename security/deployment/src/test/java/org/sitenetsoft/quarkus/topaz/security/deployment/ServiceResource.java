package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
    @Path("reactive/{todo}")
    public Uni<String> deleteReactive(@PathParam("todo") String todo) {
        return service.deleteReactive(todo);
    }

    @DELETE
    @Path("blocking/{todo}")
    @Blocking
    public String deleteBlocking(@PathParam("todo") String todo) {
        return service.deleteBlocking(todo);
    }

    @POST
    @Path("ref")
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    public String deleteRef(SecuredService.TodoRef ref) {
        return service.deleteRef(ref);
    }
}
