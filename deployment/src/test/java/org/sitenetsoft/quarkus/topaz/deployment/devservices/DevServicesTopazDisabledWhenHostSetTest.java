package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazDisabledWhenHostSetTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.authorizer.host", "example.invalid")
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.deadline", "1s");

    @Inject
    TopazClient topaz;

    @Test
    void noContainerStarted() {
        assertThat(ConfigProvider.getConfig().getOptionalValue("quarkus.topaz.console-url", String.class)).isEmpty();
        assertThatThrownBy(() -> topaz.authorizer().info().await().atMost(ofSeconds(10)))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
                .isEqualTo(Status.Code.UNAVAILABLE);
    }
}
