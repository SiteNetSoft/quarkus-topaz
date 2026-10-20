package org.sitenetsoft.quarkus.topaz.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TodoResourceTest {

    private static final String RICK = "rick@the-citadel.com";
    private static final String MORTY = "morty@the-citadel.com";

    @Test
    void rickCanDelete() {
        given().queryParam("user", RICK)
                .when().get("/todos/todo-1/can-delete")
                .then().statusCode(200).body("allowed", is(true));
    }

    @Test
    void mortyCannotDelete() {
        given().queryParam("user", MORTY)
                .when().get("/todos/todo-1/can-delete")
                .then().statusCode(200).body("allowed", is(false));
    }

    @Test
    void unknownUserIs403() {
        given().queryParam("user", "evil@the-citadel.com")
                .when().get("/todos/todo-1/can-delete")
                .then().statusCode(403)
                // the gRPC status code Topaz answered with, not a connectivity failure
                .body("error", matchesPattern("^[A-Z_]+$"))
                .body("error", not(oneOf("UNAVAILABLE", "DEADLINE_EXCEEDED")));
    }

    @Test
    void missingUserParamIs400() {
        given().when().get("/todos/todo-1/can-delete").then().statusCode(400);
    }

    @Test
    void unknownTodoIdIsDenied() {
        given().queryParam("user", RICK)
                .when().get("/todos/nope/can-delete")
                .then().statusCode(200).body("allowed", is(false));
    }

    @Test
    void ownerCheck() {
        given().when().get("/todos/todo-1/owner/{user}", RICK)
                .then().statusCode(200).body("owner", is(true));
        given().when().get("/todos/todo-1/owner/{user}", MORTY)
                .then().statusCode(200).body("owner", is(false));
    }

    @Test
    void setOwnerThenCheck() {
        given().when().post("/todos/todo-2/owner/{user}", MORTY)
                .then().statusCode(204);
        given().when().get("/todos/todo-2/owner/{user}", MORTY)
                .then().statusCode(200).body("owner", is(true));
    }

    @Test
    void readyIncludesTopaz() {
        given().when().get("/q/health/ready")
                .then().statusCode(200)
                .body("checks.find { it.name == 'Topaz' }.status", is("UP"))
                .body("checks.name", hasItem("Topaz"));
    }
}
