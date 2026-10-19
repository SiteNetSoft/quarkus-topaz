package org.sitenetsoft.quarkus.topaz.deployment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class TopazHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("topaz"))
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data");

    @Test
    void upWithDevService() {
        RestAssured.get("/q/health/ready").then().statusCode(200)
                .body("checks.find { it.name == 'Topaz' }.status", equalTo("UP"))
                .body("checks.find { it.name == 'Topaz' }.data.authorizer", matchesPattern(".+:\\d+"))
                .body("checks.find { it.name == 'Topaz' }.data.directory", matchesPattern(".+:\\d+"));
    }
}
