package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazNoDataFilesTest {

    // A data-location with neither objects.json nor relations.json is a misconfiguration and fails the build.
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("topaz-nodata"))
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz-nodata/data")
            .assertException(t -> {
                Throwable c = t;
                while (c != null && !(c instanceof ConfigurationException)) {
                    c = c.getCause();
                }
                assertThat(c).isInstanceOf(ConfigurationException.class);
                assertThat(c.getMessage()).contains("neither objects.json nor relations.json");
            });

    @Test
    void dataDirectoryWithoutSeedFilesFailsTheBuild() {
    }
}
