package org.sitenetsoft.quarkus.topaz.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.zanzibar.Relationship;
import io.quarkiverse.zanzibar.RelationshipManager;
import io.quarkiverse.zanzibar.annotations.FGAPathObject;
import io.quarkiverse.zanzibar.annotations.FGARelation;
import io.quarkiverse.zanzibar.annotations.FGAUserType;
import io.smallrye.mutiny.Uni;

@Path("/fga/todos")
@ApplicationScoped
@FGAUserType("user")
public class FgaTodoResource {

    @Inject
    RelationshipManager manager;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("owner")
    public Map<String, String> get(@PathParam("id") String id) {
        return Map.of("id", id);
    }

    @DELETE
    @Path("/{id}")
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("can_delete")
    public Response delete(@PathParam("id") String id) {
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/owners/{user}")
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("owner")
    public Uni<Response> share(@PathParam("id") String id, @PathParam("user") String user) {
        return manager.add(List.of(Relationship.of("todo", id, "owner", "user", user)))
                .map(v -> Response.noContent().build());
    }

    @DELETE
    @Path("/{id}/owners/{user}")
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("owner")
    public Uni<Response> unshare(@PathParam("id") String id, @PathParam("user") String user) {
        return manager.remove(List.of(Relationship.of("todo", id, "owner", "user", user)))
                .map(v -> Response.noContent().build());
    }
}
