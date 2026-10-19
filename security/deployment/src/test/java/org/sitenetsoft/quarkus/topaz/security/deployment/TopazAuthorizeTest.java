package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

// No HTTP policy: only the @TopazAuthorize interceptor enforces.
public class TopazAuthorizeTest {

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
    void reactiveOwnerPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/reactive/todo-1").then().statusCode(200).body(equalTo("deleted todo-1"));
    }

    @Test
    void reactiveNonOwnerForbidden() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/reactive/todo-1").then().statusCode(403);
    }

    @Test
    void blockingOwnerPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/blocking/todo-1").then().statusCode(200).body(equalTo("deleted todo-1"));
    }

    @Test
    void blockingNonOwnerForbidden() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/blocking/todo-1").then().statusCode(403);
    }

    @Test
    void anonymousUnauthorized() {
        given().get("/service/list").then().statusCode(401);
    }

    @Test
    void unknownUserForbidden() {
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .get("/service/list").then().statusCode(403);
    }

    @Test
    void knownUserListPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/service/list").then().statusCode(200).body(equalTo("ok"));
    }

    @Test
    void derivedPathFromRequest() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos/derived").then().statusCode(200).body(equalTo("derived"));
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .get("/todos/derived").then().statusCode(403);
    }
}
