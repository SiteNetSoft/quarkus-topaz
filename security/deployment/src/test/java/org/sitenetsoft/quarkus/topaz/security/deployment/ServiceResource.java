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
import io.smallrye.mutiny.Multi;
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

    @DELETE
    @Path("owned/{todo}")
    public Uni<String> deleteOwned(@PathParam("todo") String todo) {
        return service.deleteOwned(todo);
    }

    @POST
    @Path("owned-dto")
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    public String deleteOwnedDto(SecuredService.OwnedTodo todo) {
        return service.deleteOwnedDto(todo);
    }

    @DELETE
    @Path("owned-fixed")
    @Blocking
    public String deleteFixed() {
        return service.deleteFixed();
    }

    @DELETE
    @Path("owned-blank")
    public Uni<String> deleteOwnedBlank() {
        return service.deleteOwned("");
    }

    @GET
    @Path("stream")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<String> stream() {
        return service.streamTodos();
    }

    @GET
    @Path("stream-count")
    public String streamCount() {
        return String.valueOf(service.streamEntered());
    }

    @GET
    @Path("visible")
    @Blocking
    public String visible() {
        return service.peekVisible();
    }

    @GET
    @Path("visible-as-allowed")
    @Blocking
    public String visibleAsAllowed() {
        return service.peekAllowed();
    }
}
