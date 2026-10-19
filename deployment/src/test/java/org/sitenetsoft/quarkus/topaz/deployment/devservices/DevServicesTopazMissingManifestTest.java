package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazMissingManifestTest {

    // A failing build step is rethrown, not logged, so the message is asserted on the exception chain.
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:nope.yaml")
            .assertException(t -> {
                Throwable c = t;
                while (c != null && !(c instanceof ConfigurationException)) {
                    c = c.getCause();
                }
                assertThat(c).isInstanceOf(ConfigurationException.class);
                assertThat(c.getMessage()).contains("quarkus.topaz.devservices.manifest-location")
                        .contains("classpath:nope.yaml");
            });

    @Test
    void missingManifestFailsBuild() {
    }
}
