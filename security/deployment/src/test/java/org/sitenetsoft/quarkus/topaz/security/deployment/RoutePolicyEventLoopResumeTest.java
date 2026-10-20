package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.DecisionCache;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A permitted request continues on the event loop, with the context locals set by an earlier route filter, even when a
 * {@link DecisionCache} answers synchronously.
 */
public class RoutePolicyEventLoopResumeTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ResumeRoutes.class, AlwaysTrueCache.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.http.auth.permission.resume.paths", "/resume/*")
            .overrideConfigKey("quarkus.http.auth.permission.resume.policy", "topaz");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void cacheHitResumesOnEventLoop() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/resume/where").then().statusCode(200).body(org.hamcrest.Matchers.equalTo("true|set"));
    }

    @Singleton
    public static class AlwaysTrueCache implements DecisionCache {

        @Override
        public Optional<Boolean> get(String key) {
            return Optional.of(true);
        }

        @Override
        public void put(String key, boolean allowed) {
        }
    }
}
