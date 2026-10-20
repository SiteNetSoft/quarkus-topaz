package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.IdentityContexts;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;

import aserto.authorizer.v2.api.IdentityContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Mappers run on a worker thread, but under the request's Vert.x duplicated context, which stays current. The CDI
 * request scope is not active while HTTP security policies run, so it is deliberately not tested here.
 */
public class RoutePolicyMapperContextTest {

    static final AtomicReference<String> THREAD = new AtomicReference<>();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class, ContextIdentityMapper.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void mapperKeepsRequestContext() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(200);
        assertThat(THREAD.get()).doesNotContain("eventloop");
    }

    @Singleton
    public static class ContextIdentityMapper implements IdentityMapper {

        @Override
        public Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request) {
            // fails (and so denies) without the request's duplicated Vert.x context
            Context context = Vertx.currentContext();
            if (context == null || !VertxContext.isDuplicatedContext(context)) {
                return Uni.createFrom().failure(new IllegalStateException("no request context"));
            }
            THREAD.set(Thread.currentThread().getName());
            return Uni.createFrom().item(IdentityContexts.subject(identity.getPrincipal().getName()));
        }
    }
}
