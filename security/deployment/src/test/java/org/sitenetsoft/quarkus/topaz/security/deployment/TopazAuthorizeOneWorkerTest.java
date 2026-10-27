package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;

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
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class TopazAuthorizeOneWorkerTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ServiceResource.class, SecuredService.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class, SubjectIdentityMapper.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.thread-pool.core-threads", "1")
            .overrideConfigKey("quarkus.thread-pool.max-threads", "1");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void blockingOwnerPermittedWithOneWorker() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/blocking/todo-1").then().statusCode(200).body(equalTo("deleted todo-1"));
    }

    @Singleton
    public static class SubjectIdentityMapper implements IdentityMapper {

        @Override
        public Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request) {
            return Uni.createFrom().item(IdentityContexts.subject(identity.getPrincipal().getName()));
        }
    }
}
