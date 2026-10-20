package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import jakarta.inject.Singleton;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;
import org.sitenetsoft.quarkus.topaz.security.runtime.DecisionEvaluator;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.RoutingContext;

/**
 * The enforcement timeout bounds a blocking custom PolicyPathMapper too.
 */
public class RoutePolicySlowPathMapperTest {

    static final CountDownLatch RELEASE = new CountDownLatch(1);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class, SlowPolicyPathMapper.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "1s")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz")
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.WARNING.intValue()
                    && DecisionEvaluator.class.getName().equals(r.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> ((ExtLogRecord) r).getFormattedMessage()
                    .contains("<unresolved policy path for GET /todos>")));

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void slowPathMapperIsDeniedWithinTheTimeout() throws InterruptedException {
        long start = System.nanoTime();
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(403);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(4000);
        RELEASE.countDown();
        Thread.sleep(300);
    }

    @Singleton
    public static class SlowPolicyPathMapper implements PolicyPathMapper {

        @Override
        public String map(RoutingContext request, Optional<ResourceMethodInfo> method) {
            try {
                RELEASE.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "todoApp.GET.todos";
        }
    }
}
