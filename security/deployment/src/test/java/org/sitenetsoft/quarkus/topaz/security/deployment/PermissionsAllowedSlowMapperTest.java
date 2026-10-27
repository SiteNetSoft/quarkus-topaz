package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.SubjectMapper;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class PermissionsAllowedSlowMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PermissionsResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class, SlowSubjectMapper.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "1s")
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.enabled", "true");

    static final CountDownLatch DONE = new CountDownLatch(1);
    static volatile String thread;

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void relationDeniesOffTheEventLoop() throws InterruptedException {
        long start = System.nanoTime();
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/perm/todos/todo-1").then().statusCode(403);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(3000);
        assertThat(DONE.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(thread).isNotNull().doesNotStartWith("vert.x-eventloop");
    }

    @Singleton
    public static class SlowSubjectMapper implements SubjectMapper {

        @Override
        public Uni<String> map(SecurityIdentity identity) {
            thread = Thread.currentThread().getName();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                DONE.countDown();
            }
            return Uni.createFrom().item(identity.getPrincipal().getName());
        }
    }
}
