package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * The authorizer accepts connections and never answers, and no gRPC deadline is set: only the enforcement timeout can
 * end the call.
 */
public class RoutePolicyTopazDownTest {

    private static final ServerSocket SILENT = SilentListener.open();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class, SilentListener.class))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "2s")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.WARNING.intValue()
                    && r.getLoggerName().endsWith("DecisionEvaluator"))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> r.getMessage().contains("todoApp.GET.todos")
                    || Arrays.toString(r.getParameters()).contains("todoApp.GET.todos")));

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @AfterAll
    static void close() throws IOException {
        SILENT.close();
    }

    @Test
    void deniedByEnforcementTimeout() {
        long t0 = System.nanoTime();
        given().auth().preemptive().basic("rick@the-citadel.com", "rick").get("/todos").then().statusCode(403);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofSeconds(2)).isLessThan(Duration.ofSeconds(5));
    }
}
