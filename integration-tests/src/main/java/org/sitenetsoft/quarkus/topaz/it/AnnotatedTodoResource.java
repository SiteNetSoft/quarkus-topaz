package org.sitenetsoft.quarkus.topaz.it;

import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;
import org.sitenetsoft.quarkus.topaz.security.TopazCheck;
import org.sitenetsoft.quarkus.topaz.security.TopazObjectId;

import io.quarkus.security.identity.SecurityIdentityAugmentor;

/**
 * Guarded by the interceptors instead of the route policy ({@code /annotated} is outside its paths).
 */
@Path("/annotated")
@Produces(MediaType.APPLICATION_JSON)
public class AnnotatedTodoResource {

    @Inject
    @Any
    Instance<SecurityIdentityAugmentor> augmentors;

    @GET
    @Path("todos")
    @TopazAuthorize(path = "todoApp.GET.todos")
    public List<String> list() {
        return List.of("todo-1");
    }

    @DELETE
    @Path("todos/{id}")
    @TopazCheck(objectType = "todo", relation = "owner")
    public void delete(@TopazObjectId @PathParam("id") String id) {
        // no-op: the test asserts the authorization outcome, not the deletion
    }

    /** Bean classes of the installed augmentors, so tests (including the native IT) can assert which are present. */
    @GET
    @Path("augmentors")
    public List<String> augmentors() {
        return augmentors.handlesStream().map(h -> h.getBean().getBeanClass().getName()).sorted().toList();
    }
}
