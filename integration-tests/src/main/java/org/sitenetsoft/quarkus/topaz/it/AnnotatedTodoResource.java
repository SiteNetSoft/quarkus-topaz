package org.sitenetsoft.quarkus.topaz.it;

import java.util.List;
import java.util.Map;

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
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;

/**
 * Guarded by the interceptors instead of the route policy ({@code /annotated} is outside its paths).
 */
@Path("/annotated")
@Produces(MediaType.APPLICATION_JSON)
public class AnnotatedTodoResource {

    @Inject
    @Any
    Instance<SecurityIdentityAugmentor> augmentors;

    @Inject
    @Any
    Instance<HttpSecurityPolicy> policies;

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

    /**
     * Test-only, unauthenticated introspection endpoint: the bean classes of the installed augmentors and HTTP security
     * policies, so tests (including the native IT, which cannot use Arc) can assert which are present.
     */
    @GET
    @Path("augmentors")
    public Map<String, List<String>> augmentors() {
        return Map.of("augmentors", beanClasses(augmentors), "policies", beanClasses(policies));
    }

    private static List<String> beanClasses(Instance<?> beans) {
        return beans.handlesStream().map(h -> h.getBean().getBeanClass().getName()).sorted().toList();
    }
}
