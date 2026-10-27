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
 * Todo endpoints guarded by the Topaz annotations instead of the route policy.
 * The path {@code /annotated} is outside the policy paths.
 */
@Path("/annotated")
@Produces(MediaType.APPLICATION_JSON)
public class AnnotatedTodoResource {

    /** The installed identity augmentors. */
    @Inject
    @Any
    Instance<SecurityIdentityAugmentor> augmentors;

    /** The installed HTTP security policies. */
    @Inject
    @Any
    Instance<HttpSecurityPolicy> policies;

    /**
     * Create the resource.
     */
    public AnnotatedTodoResource() {
    }

    /**
     * List the todos, allowed by the policy path {@code todoApp.GET.todos}.
     *
     * @return the todo ids
     */
    @GET
    @Path("todos")
    @TopazAuthorize(path = "todoApp.GET.todos")
    public List<String> list() {
        return List.of("todo-1");
    }

    /**
     * Delete a todo, allowed when the caller has the {@code owner} relation on it.
     *
     * @param id the todo id
     */
    @DELETE
    @Path("todos/{id}")
    @TopazCheck(objectType = "todo", relation = "owner")
    public void delete(@TopazObjectId @PathParam("id") String id) {
        // No-op. The test asserts the authorization outcome, not the deletion.
    }

    /**
     * Report the bean classes of the installed augmentors and HTTP security policies.
     * <p>
     * This endpoint is for tests and needs no authentication.
     * Tests, including the native IT that cannot use Arc, use it to assert which beans are present.
     *
     * @return the sorted class names, under the keys {@code augmentors} and {@code policies}
     */
    @GET
    @Path("augmentors")
    public Map<String, List<String>> augmentors() {
        return Map.of("augmentors", beanClasses(augmentors), "policies", beanClasses(policies));
    }

    /**
     * Get the sorted class names of the beans in an instance.
     *
     * @param beans the instance to read
     * @return the sorted class names
     */
    private static List<String> beanClasses(Instance<?> beans) {
        return beans.handlesStream().map(h -> h.getBean().getBeanClass().getName()).sorted().toList();
    }
}
