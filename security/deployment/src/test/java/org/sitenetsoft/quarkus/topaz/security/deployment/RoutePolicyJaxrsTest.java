package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * With {@code applies-to=jaxrs} the policy runs after REST matching, so the policy path uses the template and the
 * resource context carries the path parameters.
 */
public class RoutePolicyJaxrsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos/*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz")
            .overrideConfigKey("quarkus.http.auth.permission.todos.applies-to", "jaxrs");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void ownerCanDelete() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/todos/todo-1").then().statusCode(204);
    }

    @Test
    void nonOwnerIsDenied() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/todos/todo-1").then().statusCode(403);
    }
}
