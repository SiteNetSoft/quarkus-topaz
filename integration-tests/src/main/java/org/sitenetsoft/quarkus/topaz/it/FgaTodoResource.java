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

/**
 * Todo endpoints guarded by the Quarkus Zanzibar annotations.
 * The filter checks each relation through the Topaz relationship manager.
 */
@Path("/fga/todos")
@ApplicationScoped
@FGAUserType("user")
public class FgaTodoResource {

    /** The relationship manager, backed by the Topaz Directory. */
    @Inject
    RelationshipManager manager;

    /**
     * Create the resource.
     */
    public FgaTodoResource() {
    }

    /**
     * Get a todo, allowed for an owner.
     *
     * @param id the todo id
     * @return the todo id
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("owner")
    public Map<String, String> get(@PathParam("id") String id) {
        return Map.of("id", id);
    }

    /**
     * Delete a todo, allowed when the caller has the {@code can_delete} relation.
     *
     * @param id the todo id
     * @return an empty 204 response
     */
    @DELETE
    @Path("/{id}")
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("can_delete")
    public Response delete(@PathParam("id") String id) {
        return Response.noContent().build();
    }

    /**
     * Make a user an owner of a todo, allowed for an owner.
     *
     * @param id the todo id
     * @param user the user id
     * @return an empty 204 response
     */
    @PUT
    @Path("/{id}/owners/{user}")
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("owner")
    public Uni<Response> share(@PathParam("id") String id, @PathParam("user") String user) {
        return manager.add(List.of(Relationship.of("todo", id, "owner", "user", user)))
                .map(v -> Response.noContent().build());
    }

    /**
     * Remove an owner from a todo, allowed for an owner.
     *
     * @param id the todo id
     * @param user the user id
     * @return an empty 204 response
     */
    @DELETE
    @Path("/{id}/owners/{user}")
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("owner")
    public Uni<Response> unshare(@PathParam("id") String id, @PathParam("user") String user) {
        return manager.remove(List.of(Relationship.of("todo", id, "owner", "user", user)))
                .map(v -> Response.noContent().build());
    }
}
