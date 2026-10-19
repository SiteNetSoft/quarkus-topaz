package org.sitenetsoft.quarkus.topaz.it;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Guarded only by the {@code topaz} route policy ({@code quarkus.http.auth.permission.secured.*}); the policy paths
 * come from the JAX-RS templates: {@code todoApp.GET.secured.todos} and {@code todoApp.DELETE.secured.todos.__id}.
 */
@Path("/secured/todos")
@Produces(MediaType.APPLICATION_JSON)
public class SecuredTodoResource {

    @GET
    public List<String> list() {
        return List.of("todo-1");
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) {
        // no-op: the test asserts the authorization outcome, not the deletion
    }
}
