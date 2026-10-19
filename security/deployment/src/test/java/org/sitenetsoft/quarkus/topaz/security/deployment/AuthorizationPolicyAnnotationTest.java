package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * {@code @AuthorizationPolicy(name = "topaz")} on a resource class, with no HTTP permission configured.
 */
public class AuthorizationPolicyAnnotationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(AnnotatedTodoResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp");

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

    @Test
    void listIsPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(200);
    }
}
