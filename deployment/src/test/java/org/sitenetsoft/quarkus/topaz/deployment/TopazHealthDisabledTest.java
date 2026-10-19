package org.sitenetsoft.quarkus.topaz.deployment;

import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class TopazHealthDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.health.enabled", "false");

    @Test
    void disabledWhenConfigured() {
        RestAssured.get("/q/health/ready").then()
                .body("checks.findAll { it.name == 'Topaz' }", empty());
    }
}
