package org.sitenetsoft.quarkus.topaz.deployment;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class TopazHealthDownTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "example.invalid")
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.deadline", "1s");

    @Test
    void downWhenUnreachable() {
        RestAssured.get("/q/health/ready").then().statusCode(503)
                .body("checks.find { it.name == 'Topaz' }.status", equalTo("DOWN"))
                .body("checks.find { it.name == 'Topaz' }.data.error", not(emptyOrNullString()));
    }
}
