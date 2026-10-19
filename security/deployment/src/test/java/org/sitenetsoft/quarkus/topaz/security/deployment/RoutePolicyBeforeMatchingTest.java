package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Without {@code applies-to=jaxrs} the policy runs before REST matching: no template is known, the raw path is mapped
 * and Topaz has no such policy, so the request is denied.
 */
public class RoutePolicyBeforeMatchingTest {

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
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos/*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.WARNING.intValue()
                    && r.getLoggerName().endsWith("DecisionEvaluator"))
            .assertLogRecords(records -> assertThat(records)
                    .anyMatch(r -> r.getMessage().contains("todoApp.DELETE.todos.todo-1")
                            || Arrays.toString(r.getParameters()).contains("todoApp.DELETE.todos.todo-1")));

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void pathParamsNeedJaxrsMode() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/todos/todo-1").then().statusCode(403);
    }
}
