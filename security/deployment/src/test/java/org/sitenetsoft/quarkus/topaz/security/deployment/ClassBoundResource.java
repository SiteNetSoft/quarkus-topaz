package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.common.annotation.Blocking;

@Path("/class-bound")
@Produces(MediaType.TEXT_PLAIN)
public class ClassBoundResource {

    @Inject
    AuthorizeBoundService authorizeBound;

    @Inject
    CheckBoundService checkBound;

    @DELETE
    @Path("authorize/{id}")
    @Blocking
    public String authorize(@PathParam("id") String id) {
        return authorizeBound.delete(id);
    }

    @DELETE
    @Path("check/{id}")
    @Blocking
    public String check(@PathParam("id") String id) {
        return checkBound.delete(id);
    }
}
