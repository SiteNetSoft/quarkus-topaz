package org.sitenetsoft.quarkus.topaz.security.norest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;
import org.sitenetsoft.quarkus.topaz.security.runtime.NoopResourceMethodInfoResolver;

import io.quarkus.test.junit.QuarkusTest;

/**
 * The application has no Quarkus REST: it must build, and the topaz policy must still enforce on a reactive route.
 */
@QuarkusTest
class NoRestEnforcementTest {

    @Inject
    ResourceMethodInfoResolver resolver;

    @Test
    void restResolverIsExcluded() {
        // the runtime jar is indexed: without the exclusion the REST resolver would be discovered and win
        assertInstanceOf(NoopResourceMethodInfoResolver.class, resolver);
    }

    @Test
    void knownUserIsPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(200).body(equalTo("ok"));
    }

    @Test
    void unknownUserIsDenied() {
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .get("/todos").then().statusCode(403);
    }

    @Test
    void anonymousIsUnauthorized() {
        given().get("/todos").then().statusCode(401);
    }
}
