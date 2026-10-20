package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * With {@code read-body} the mapper sees the request body; the policy allows only {@code input.resource.body == "yes"}.
 */
public class ReadBodyTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(BodyResource.class, BodyContextMapper.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.read-body", "true")
            .overrideConfigKey("quarkus.http.auth.permission.body.paths", "/body*")
            .overrideConfigKey("quarkus.http.auth.permission.body.policy", "topaz");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void bodyIsAvailableToMapper() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .contentType("text/plain").body("yes")
                .post("/body").then().statusCode(200).body(equalTo("accepted yes"));
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .contentType("text/plain").body("no")
                .post("/body").then().statusCode(403);
    }
}
