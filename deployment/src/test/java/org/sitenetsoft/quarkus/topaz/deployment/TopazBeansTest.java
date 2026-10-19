package org.sitenetsoft.quarkus.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.AuthorizerClient;
import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.QuarkusUnitTest;

public class TopazBeansTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.authorizer.port", "1")
            .overrideConfigKey("quarkus.topaz.directory.port", "1")
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.deadline", "2s")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false");

    @Inject
    TopazClient client;
    @Inject
    AuthorizerClient authorizer;
    @Inject
    DirectoryClient directory;

    @Test
    void beansInjected() {
        assertThat(client.authorizer()).isSameAs(authorizer);
        assertThat(client.directory()).isSameAs(directory);
    }

    @Test
    void unreachableFailsWithinDeadline() {
        long t0 = System.nanoTime();
        assertThatThrownBy(() -> authorizer.info().await().atMost(Duration.ofSeconds(10)))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
                .isIn(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED);
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isLessThan(Duration.ofSeconds(5));
    }
}
