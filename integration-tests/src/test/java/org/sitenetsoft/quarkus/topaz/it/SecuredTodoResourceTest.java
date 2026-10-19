package org.sitenetsoft.quarkus.topaz.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class SecuredTodoResourceTest {

    private static final String PERMISSION_AUGMENTOR = "org.sitenetsoft.quarkus.topaz.security.runtime.TopazPermissionAugmentor";

    private static RequestSpecification as(String user, String password) {
        return given().auth().preemptive().basic(user, password);
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

    @Test
    void routePolicyOwnerDeletes() {
        rick().when().delete("/secured/todos/todo-1").then().statusCode(204);
    }

    @Test
    void routePolicyNonOwnerDenied() {
        morty().when().delete("/secured/todos/todo-1").then().statusCode(403);
    }

    @Test
    void routePolicyUnknownUser() {
        // summer authenticates against the embedded realm but is not in the Topaz directory: fail closed
        summer().when().get("/secured/todos").then().statusCode(403);
    }

    @Test
    void routePolicyAnonymous() {
        given().when().get("/secured/todos").then().statusCode(401);
    }

    @Test
    void annotatedCheckOwner() {
        rick().when().delete("/annotated/todos/todo-1").then().statusCode(204);
    }

    @Test
    void annotatedCheckNonOwner() {
        morty().when().delete("/annotated/todos/todo-1").then().statusCode(403);
    }

    @Test
    void annotatedAuthorizeList() {
        rick().when().get("/annotated/todos").then().statusCode(200);
    }

    @Test
    void permissionAugmentorAbsentWithoutOptIn() {
        // quarkus.topaz.enforcement.permissions.enabled is unset: the @PermissionsAllowed bridge must not be installed
        given().when().get("/annotated/augmentors")
                .then().statusCode(200)
                .body("$", instanceOf(List.class))
                .body("$", not(hasItem(PERMISSION_AUGMENTOR)));
    }
}
