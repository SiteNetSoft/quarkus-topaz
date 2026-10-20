package org.sitenetsoft.quarkus.topaz.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * {@code @AuthorizationPolicy(name = "topaz")} without any HTTP permission config still needs the policy root.
 */
public class AuthorizationPolicyMissingRootTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(AnnotatedTodoResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .assertException(t -> assertThat(t).isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("quarkus.topaz.enforcement.policy-root must be set"));

    @Test
    void startupFails() {
        fail("should not start");
    }
}
