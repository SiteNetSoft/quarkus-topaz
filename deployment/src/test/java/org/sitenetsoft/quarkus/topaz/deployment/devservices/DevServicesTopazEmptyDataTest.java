package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazEmptyDataTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            // both seed files exist and declare no entries
            .withApplicationRoot(jar -> jar.addAsResource("topaz").addAsResource("topaz-empty"))
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz-empty/data");

    @Inject
    TopazClient topaz;

    @Test
    void emptySeedFilesStartWithNothingSeeded() {
        assertThat(topaz.authorizer().info().await().atMost(ofSeconds(10)).getVersion()).isNotBlank();
        assertThatThrownBy(() -> topaz.directory().getObject("user", "rick@the-citadel.com").await().atMost(ofSeconds(10)))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }
}
