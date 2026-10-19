package org.sitenetsoft.quarkus.topaz.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class FgaTodoResourceTest {

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
    // todo-3 is this test's own object: TodoResourceTest leaves morty as an owner of todo-2, so test order must not matter
    void shareAndUnshare() {
        morty().get("/fga/todos/todo-3").then().statusCode(403);
        rick().put("/fga/todos/todo-3/owners/morty@the-citadel.com").then().statusCode(204);
        morty().get("/fga/todos/todo-3").then().statusCode(200);
        rick().delete("/fga/todos/todo-3/owners/morty@the-citadel.com").then().statusCode(204);
        morty().get("/fga/todos/todo-3").then().statusCode(403);
    }

    @Test
    void existingEndpointsAreUnaffected() {
        given().queryParam("user", "rick@the-citadel.com").get("/todos/todo-1/can-delete").then().statusCode(200); // not FGA-annotated; deny-unannotated is off in this app
        rick().get("/secured/todos").then().statusCode(200); // still guarded by the topaz HTTP policy
    }
}
