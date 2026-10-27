package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.RoutingContext;

/**
 * An application that supplies its own {@link PolicyPathMapper} needs no policy-root.
 */
public class RoutePolicyProducedMapperNoRootTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class, Producers.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void producedMapperNeedsNoPolicyRoot() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(200);
    }

    public static class Producers {

        @Produces
        @Singleton
        PolicyPathMapper mapper() {
            return (request, method) -> "todoApp.GET.todos";
        }
    }
}
