package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazBadDataTest {

    // Seed data is parsed strictly at build time: a misspelled field fails the build, not silently dropped.
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("topaz-bad"))
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz-bad/data")
            .assertException(t -> {
                Throwable c = t;
                while (c != null && !(c instanceof ConfigurationException)) {
                    c = c.getCause();
                }
                assertThat(c).isInstanceOf(ConfigurationException.class);
                assertThat(c.getMessage()).contains("objects.json").contains("bogus");
            });

    @Test
    void unknownFieldFailsBuild() {
    }
}
