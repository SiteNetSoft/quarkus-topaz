package org.sitenetsoft.quarkus.topaz.it;

import static org.sitenetsoft.quarkus.topaz.IdentityContexts.subject;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import aserto.directory.common.v3.Relation;
import io.grpc.StatusRuntimeException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

/**
 * Todo endpoints that call the Topaz client directly.
 */
@Path("/todos/{id}")
@Produces(MediaType.APPLICATION_JSON)
public class TodoResource {

    /** The policy path that decides whether a user can delete a todo. */
    static final String DELETE_POLICY = "todoApp.DELETE.todos.__id";

    /** The Topaz client. */
    @Inject
    TopazClient topaz;

    /**
     * Create the resource.
     */
    public TodoResource() {
    }

    /**
     * Ask the authorizer whether a user can delete a todo.
     *
     * @param id the todo id
     * @param user the user id
     * @return {@code allowed}, or 400 without a user, or 403 when Topaz rejects the identity
     */
    @GET
    @Path("can-delete")
    public Uni<RestResponse<Map<String, Object>>> canDelete(@PathParam("id") String id, @QueryParam("user") String user) {
        if (user == null || user.isBlank()) {
            return Uni.createFrom().item(RestResponse.status(Response.Status.BAD_REQUEST,
                    Map.<String, Object> of("error", "user query parameter is required")));
        }
        return topaz.authorizer().isAllowed(subject(user), DELETE_POLICY, "allowed", Map.of("id", id))
                .map(allowed -> RestResponse.ok(Map.<String, Object> of("allowed", allowed)))
                // Topaz rejects identities it cannot resolve. Answer 403 with the gRPC status code.
                .onFailure(StatusRuntimeException.class)
                .recoverWithItem(e -> {
                    var status = ((StatusRuntimeException) e).getStatus();
                    Log.warnf("Topaz rejected identity %s: %s %s", user, status.getCode(), status.getDescription());
                    return RestResponse.status(Response.Status.FORBIDDEN,
                            Map.<String, Object> of("error", status.getCode().name()));
                });
    }

    /**
     * Check whether a user owns a todo.
     *
     * @param id the todo id
     * @param user the user id
     * @return {@code owner}, true when the user owns the todo
     */
    @GET
    @Path("owner/{user}")
    public Uni<Map<String, Boolean>> isOwner(@PathParam("id") String id, @PathParam("user") String user) {
        return topaz.directory().check("todo", id, "owner", "user", user)
                .map(owner -> Map.of("owner", owner));
    }

    /**
     * Make a user the owner of a todo.
     *
     * @param id the todo id
     * @param user the user id
     * @return completion of the Directory write
     */
    @POST
    @Path("owner/{user}")
    public Uni<Void> setOwner(@PathParam("id") String id, @PathParam("user") String user) {
        return topaz.directory().setRelation(Relation.newBuilder()
                .setObjectType("todo").setObjectId(id)
                .setRelation("owner")
                .setSubjectType("user").setSubjectId(user)
                .build())
                .replaceWithVoid();
    }
}
