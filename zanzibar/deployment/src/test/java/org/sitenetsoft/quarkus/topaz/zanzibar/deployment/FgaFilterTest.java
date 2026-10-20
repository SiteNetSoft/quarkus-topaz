package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.specification.RequestSpecification;

public class FgaFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(FgaTodoResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            // quarkus.zanzibar.extract-user-type-from-roles defaults to true: when the user type is not in the principal name and
            // the identity has exactly one role, DefaultUserExtractor.extractUserTypeFromRoles takes that role as the Topaz
            // subject type. TestIdentityController.add(user, password) without roles builds the identity with the role set
            // {null}, so Optional.of(null) throws an NPE (every request answers 500). That is specific to this test helper.
            // A real application with a single-role user is not hit by the NPE, but the role name (say "admin") would become
            // the subject type of the check, which is a misconfiguration for Topaz (the subject type is a directory object
            // type such as "user", not a role). Applications should leave this false or use @FGAUserType.
            .overrideConfigKey("quarkus.zanzibar.extract-user-type-from-roles", "false")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void ownerCanRead() {
        rick().get("/fga/todos/todo-1").then().statusCode(200).body("id", equalTo("todo-1"));
    }

    @Test
    void nonOwnerIsForbidden() {
        morty().get("/fga/todos/todo-1").then().statusCode(403);
    }

    @Test
    void unknownUserIsForbidden() {
        summer().get("/fga/todos/todo-1").then().statusCode(403);
    }

    @Test
    void anonymousIsForbidden() {
        // quarkus-zanzibar denies without a principal; it does not challenge
        given().get("/fga/todos/todo-1").then().statusCode(403);
    }

    @Test
    void permissionRelation() {
        rick().delete("/fga/todos/todo-1").then().statusCode(204);
        morty().delete("/fga/todos/todo-1").then().statusCode(403);
    }

    @Test
    // todo-3 is owned by rick and used only by the share tests, so no other test can change what they see
    void shareAndUnshare() {
        morty().get("/fga/todos/todo-3").then().statusCode(403);
        rick().put("/fga/todos/todo-3/owners/morty@the-citadel.com").then().statusCode(204);
        morty().get("/fga/todos/todo-3").then().statusCode(200);
        rick().delete("/fga/todos/todo-3/owners/morty@the-citadel.com").then().statusCode(204);
        morty().get("/fga/todos/todo-3").then().statusCode(403);
    }

    @Test
    void nonOwnerCannotShare() {
        // morty tries to make himself an owner of rick's todo
        morty().put("/fga/todos/todo-3/owners/morty@the-citadel.com").then().statusCode(403);
        morty().get("/fga/todos/todo-3").then().statusCode(403); // nothing was written
    }

    @Test
    void nonOwnerCannotUnshare() {
        morty().delete("/fga/todos/todo-3/owners/rick@the-citadel.com").then().statusCode(403);
        rick().get("/fga/todos/todo-3").then().statusCode(200); // rick is still an owner
    }

    @Test
    void anonymousCannotShare() {
        given().put("/fga/todos/todo-3/owners/morty@the-citadel.com").then().statusCode(403);
        morty().get("/fga/todos/todo-3").then().statusCode(403);
    }

    @Test
    void anonymousCannotUnshare() {
        given().delete("/fga/todos/todo-3/owners/rick@the-citadel.com").then().statusCode(403);
        rick().get("/fga/todos/todo-3").then().statusCode(200);
    }

    @Test
    void unannotatedEndpointIsDenied() {
        rick().get("/fga/todos/unannotated").then().statusCode(403);
    }

    @Test
    void ignoredEndpointIsOpen() {
        given().get("/fga/todos/ignored").then().statusCode(200).body(equalTo("ignored"));
    }

    private static RequestSpecification rick() {
        return as("rick@the-citadel.com", "rick");
    }

    private static RequestSpecification morty() {
        return as("morty@the-citadel.com", "morty");
    }

    private static RequestSpecification summer() {
        return as("summer@the-citadel.com", "summer");
    }

    private static RequestSpecification as(String user, String password) {
        return given().auth().preemptive().basic(user, password);
    }
}
