package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazPolicyDirAndImageTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("topaz"))
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.policy.image", "example.invalid/policy:1")
            .assertException(t -> {
                Throwable c = t;
                while (c != null && !(c instanceof ConfigurationException)) {
                    c = c.getCause();
                }
                assertThat(c).isInstanceOf(ConfigurationException.class);
                assertThat(c.getMessage()).contains("quarkus.topaz.devservices.policy.directory")
                        .contains("quarkus.topaz.devservices.policy.image").contains("mutually exclusive");
            });

    @Test
    void directoryAndImageTogetherFailTheBuild() {
    }
}
