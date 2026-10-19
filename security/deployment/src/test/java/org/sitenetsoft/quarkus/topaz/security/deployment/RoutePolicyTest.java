package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.IdentityContexts;
import org.sitenetsoft.quarkus.topaz.security.runtime.DecisionEvaluator;

import com.google.protobuf.Struct;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

public class RoutePolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz");

    @Inject
    DecisionEvaluator evaluator;

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void knownUserIsPermitted() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(200).body(equalTo("{}"));
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

    @Test
    void anonymousDenyShortCircuitsBeforeTopaz() {
        // the policy would allow anyone; anonymous=deny rejects without asking Topaz
        given().get("/todos/public").then().statusCode(401);
    }

    @Test
    void evaluateNeverThrows() {
        DecisionEvaluator.Outcome outcome = evaluator.evaluate(IdentityContexts.none(), null, Struct.getDefaultInstance())
                .await().atMost(Duration.ofSeconds(10));
        assertThat(outcome.permitted()).isFalse();
        assertThat(outcome.decisions()).isEmpty();
    }
}
