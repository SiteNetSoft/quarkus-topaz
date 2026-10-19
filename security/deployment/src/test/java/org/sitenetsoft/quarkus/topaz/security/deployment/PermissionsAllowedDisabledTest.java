package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

// quarkus.topaz.enforcement.permissions.enabled is unset: the bridge must not be installed.
public class PermissionsAllowedDisabledTest {

    static final String AUGMENTOR = "org.sitenetsoft.quarkus.topaz.security.runtime.TopazPermissionAugmentor";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PermissionsResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void noRemoteCallsWithoutOptIn() {
        assertFalse(Arc.container().select(SecurityIdentityAugmentor.class).handlesStream()
                .anyMatch(h -> h.getBean().getBeanClass().getName().equals(AUGMENTOR)));
        // the identity carries no permissions and nothing asks Topaz
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/perm/list").then().statusCode(403);
    }
}
