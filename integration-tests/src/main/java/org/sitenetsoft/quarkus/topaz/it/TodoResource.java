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

@Path("/todos/{id}")
@Produces(MediaType.APPLICATION_JSON)
public class TodoResource {

    static final String DELETE_POLICY = "todoApp.DELETE.todos.__id";

    @Inject
    TopazClient topaz;

    @GET
    @Path("can-delete")
    public Uni<RestResponse<Map<String, Object>>> canDelete(@PathParam("id") String id, @QueryParam("user") String user) {
        if (user == null || user.isBlank()) {
            return Uni.createFrom().item(RestResponse.status(Response.Status.BAD_REQUEST,
                    Map.<String, Object> of("error", "user query parameter is required")));
        }
        return topaz.authorizer().isAllowed(subject(user), DELETE_POLICY, "allowed", Map.of("id", id))
                .map(allowed -> RestResponse.ok(Map.<String, Object> of("allowed", allowed)))
                // Topaz rejects identities it cannot resolve; surface that as 403 with the gRPC status code
                .onFailure(StatusRuntimeException.class)
                .recoverWithItem(e -> {
                    var status = ((StatusRuntimeException) e).getStatus();
                    Log.warnf("Topaz rejected identity %s: %s %s", user, status.getCode(), status.getDescription());
                    return RestResponse.status(Response.Status.FORBIDDEN,
                            Map.<String, Object> of("error", status.getCode().name()));
                });
    }

    @GET
    @Path("owner/{user}")
    public Uni<Map<String, Boolean>> isOwner(@PathParam("id") String id, @PathParam("user") String user) {
        return topaz.directory().check("todo", id, "owner", "user", user)
                .map(owner -> Map.of("owner", owner));
    }

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
