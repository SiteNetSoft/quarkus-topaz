package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.runtime.DecisionEvaluator;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A denial is logged at WARN with the policy path but without the subject, which is personal data.
 */
public class RoutePolicyDenialLogTest {

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
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz")
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.WARNING.intValue()
                    && DecisionEvaluator.class.getName().equals(r.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records).anyMatch(r -> ((ExtLogRecord) r).getFormattedMessage().contains("todoApp.GET.todos"));
                assertThat(records).noneMatch(
                        r -> ((ExtLogRecord) r).getFormattedMessage().contains("summer@the-citadel.com"));
            });

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void denialLogDoesNotNameSubject() {
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .get("/todos").then().statusCode(403);
    }
}
