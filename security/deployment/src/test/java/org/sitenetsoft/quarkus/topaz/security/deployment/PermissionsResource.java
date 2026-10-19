package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.sitenetsoft.quarkus.topaz.security.TopazRelation;

import io.quarkus.security.PermissionsAllowed;

@Path("/perm")
@Produces(MediaType.TEXT_PLAIN)
public class PermissionsResource {

    @GET
    @Path("/list")
    @PermissionsAllowed("todoApp.GET.todos")
    public String list() {
        return "list";
    }

    @DELETE
    @Path("/todos/{id}")
    @PermissionsAllowed(value = "todo#owner", permission = TopazRelation.class, params = "id")
    public String delete(@PathParam("id") String id) {
        return "deleted " + id;
    }

    // malformed name: no '#', so TopazRelation rejects it when the permission is built on invocation
    @DELETE
    @Path("/malformed/{id}")
    @PermissionsAllowed(value = "todo", permission = TopazRelation.class, params = "id")
    public String malformed(@PathParam("id") String id) {
        return "deleted " + id;
    }
}
