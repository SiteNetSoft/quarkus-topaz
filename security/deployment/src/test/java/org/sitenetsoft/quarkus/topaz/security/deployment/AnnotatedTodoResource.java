package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.vertx.http.security.AuthorizationPolicy;

// Self-contained rather than extending TodoTestResource: the superclass would have to be in the archive and would
// register a second resource on /todos.
@Path("/todos")
@AuthorizationPolicy(name = "topaz")
public class AnnotatedTodoResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String list() {
        return "todos";
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) {
    }
}
