package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

// No HTTP policy: only the @TopazCheck interceptor enforces. rick owns todo-1, morty does not, summer is unknown.
public class TopazCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, ServiceResource.class, SecuredService.class,
                            TestIdentities.class, TestIdentityProvider.class, TestIdentityController.class)
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
    void ownerPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/owned/todo-1").then().statusCode(200).body(equalTo("deleted todo-1"));
    }

    @Test
    void nonOwnerForbidden() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/owned/todo-1").then().statusCode(403);
    }

    @Test
    void dtoObjectIdPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .contentType("application/json").body("{\"id\":\"todo-1\",\"note\":\"x\"}")
                .post("/service/owned-dto").then().statusCode(200).body(equalTo("deleted todo-1"));
    }

    @Test
    void dtoObjectIdNonOwnerForbidden() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .contentType("application/json").body("{\"id\":\"todo-1\",\"note\":\"x\"}")
                .post("/service/owned-dto").then().statusCode(403);
    }

    @Test
    void fixedObjectId() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/owned-fixed").then().statusCode(200).body(equalTo("deleted todo-1"));
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/owned-fixed").then().statusCode(403);
    }

    @Test
    void anonymousIsUnauthorized() {
        given().delete("/service/owned/todo-1").then().statusCode(401);
    }

    @Test
    void unknownSubjectForbidden() {
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .delete("/service/owned/todo-1").then().statusCode(403);
    }

    @Test
    void blankObjectIdForbidden() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/owned-blank").then().statusCode(403);
    }
}
