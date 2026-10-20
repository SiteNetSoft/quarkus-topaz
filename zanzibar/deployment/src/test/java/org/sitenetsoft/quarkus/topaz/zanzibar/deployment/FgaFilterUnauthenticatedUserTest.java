package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FgaFilterUnauthenticatedUserTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(FgaPublicResource.class).addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.zanzibar.filter.unauthenticated-user", "rick@the-citadel.com")
            .overrideConfigKey("quarkus.zanzibar.extract-user-type-from-roles", "false")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data");

    @Test
    void anonymousIsTreatedAsTheConfiguredUserWithTheDefaultUserType() {
        given().get("/fga/public/todo-1").then().statusCode(200);
        given().get("/fga/public/todo-2").then().statusCode(403);
    }
}
