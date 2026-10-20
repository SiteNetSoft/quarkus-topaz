package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.zanzibar.annotations.FGAPathObject;
import io.quarkiverse.zanzibar.annotations.FGARelation;

/** No {@code @FGAUserType}: the subject type comes from {@code quarkus.topaz.zanzibar.default-user-type}. */
@Path("/fga/public")
@ApplicationScoped
public class FgaPublicResource {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @FGAPathObject(param = "id", type = "todo")
    @FGARelation("owner")
    public Map<String, String> get(@PathParam("id") String id) {
        return Map.of("id", id);
    }
}
