package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.logging.Level;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jboss.logmanager.ExtLogRecord;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DecisionEvaluator;

import aserto.authorizer.v2.api.IdentityContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * The enforcement timeout also bounds the mappers: a mapper that blocks for longer must still be answered with 403.
 */
public class RoutePolicySlowMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class, SlowIdentityMapper.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "500ms")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz")
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.WARNING.intValue()
                    && DecisionEvaluator.class.getName().equals(r.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .anyMatch(r -> ((ExtLogRecord) r).getFormattedMessage().contains("todoApp.GET.todos")
                            && ((ExtLogRecord) r).getFormattedMessage().contains("TimeoutException")));

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void slowMapperIsDeniedWithinTheTimeout() {
        long start = System.nanoTime();
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(403);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(1500);
    }

    @Singleton
    public static class SlowIdentityMapper implements IdentityMapper {

        @Override
        public Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Uni.createFrom().item(org.sitenetsoft.quarkus.topaz.IdentityContexts.subject(
                    identity.getPrincipal().getName()));
        }
    }
}
