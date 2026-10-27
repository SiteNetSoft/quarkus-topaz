package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.IdentityContexts;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;

import aserto.authorizer.v2.api.IdentityContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class TopazAuthorizeSlowMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ServiceResource.class, SecuredService.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class, SlowIdentityMapper.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "1s");

    static volatile CountDownLatch done;
    static volatile String thread;

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @BeforeEach
    void reset() {
        done = new CountDownLatch(1);
        thread = null;
    }

    @Test
    void reactiveDeniesOffTheEventLoop() throws InterruptedException {
        long start = System.nanoTime();
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/reactive/todo-1").then().statusCode(403);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(3000);
        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(thread).isNotNull().doesNotStartWith("vert.x-eventloop");
    }

    @Test
    void blockingDeniesWithinTimeoutAndGrace() throws InterruptedException {
        long start = System.nanoTime();
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/blocking/todo-1").then().statusCode(403);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(4000);
        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
    }

    @Singleton
    public static class SlowIdentityMapper implements IdentityMapper {

        @Override
        public Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request) {
            thread = Thread.currentThread().getName();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
            return Uni.createFrom().item(IdentityContexts.subject(identity.getPrincipal().getName()));
        }
    }
}
