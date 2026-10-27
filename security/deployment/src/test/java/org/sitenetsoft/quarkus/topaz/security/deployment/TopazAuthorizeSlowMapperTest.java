package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class TopazAuthorizeSlowMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ServiceResource.class, SecuredService.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class, SlowIdentityMapper.class,
                            EndpointThreadResource.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "1s");

    static volatile CountDownLatch done;
    static volatile String thread;
    static volatile String endpointThread;

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @BeforeEach
    void reset() {
        done = new CountDownLatch(1);
        thread = null;
        endpointThread = null;
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
                .delete("/endpoint-thread/todo-1").then().statusCode(403);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(4000);
        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(thread).isNotNull().doesNotStartWith("vert.x-eventloop").isEqualTo(endpointThread);
    }

    @Path("/endpoint-thread")
    public static class EndpointThreadResource {

        @Inject
        SecuredService service;

        @DELETE
        @Path("{todo}")
        @Blocking
        public String delete(@PathParam("todo") String todo) {
            endpointThread = Thread.currentThread().getName();
            return service.deleteBlocking(todo);
        }
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
