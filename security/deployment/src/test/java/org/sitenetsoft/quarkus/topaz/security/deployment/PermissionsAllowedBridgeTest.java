package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

public class PermissionsAllowedBridgeTest {

    static final String AUGMENTOR = "org.sitenetsoft.quarkus.topaz.security.runtime.TopazPermissionAugmentor";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PermissionsResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.enabled", "true");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    static boolean augmentorRegistered() {
        return Arc.container().select(SecurityIdentityAugmentor.class).handlesStream()
                .anyMatch(h -> h.getBean().getBeanClass().getName().equals(AUGMENTOR));
    }

    @Test
    void augmentorPresent() {
        assertTrue(augmentorRegistered());
    }

    @Test
    void policyPermission() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/perm/list").then().statusCode(200).body(equalTo("list"));
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .get("/perm/list").then().statusCode(403);
    }

    @Test
    void relationPermissionOwner() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/perm/todos/todo-1").then().statusCode(200).body(equalTo("deleted todo-1"));
    }

    @Test
    void relationPermissionDenied() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/perm/todos/todo-1").then().statusCode(403);
    }

    @Test
    void relationPermissionUnknownSubjectDenied() {
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .delete("/perm/todos/todo-1").then().statusCode(403);
    }

    @Test
    void anonymousUnauthorized() {
        given().get("/perm/list").then().statusCode(401);
    }

    // Quarkus builds the TopazRelation on invocation; the constructor rejects the name and Quarkus denies
    @Test
    void malformedRelationNameForbidden() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/perm/malformed/todo-1").then().statusCode(403);
    }
}
