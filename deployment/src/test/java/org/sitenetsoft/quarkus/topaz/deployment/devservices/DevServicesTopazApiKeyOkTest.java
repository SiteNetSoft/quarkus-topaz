package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazApiKeyOkTest {

    // The manifest is seeded through the key too, so reading it back proves both the seeder and the app use it.
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            // classpath: locations resolve against the application, like src/main/resources in an app
            .withApplicationRoot(jar -> jar.addAsResource("topaz"))
            .overrideConfigKey("quarkus.topaz.devservices.api-key", "secret")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml");

    @Inject
    TopazClient topaz;

    @Test
    void injectedApiKeyIsAccepted() {
        assertThat(topaz.directory().getManifest().await().atMost(ofSeconds(10))).contains("can_delete: owner");
    }
}
