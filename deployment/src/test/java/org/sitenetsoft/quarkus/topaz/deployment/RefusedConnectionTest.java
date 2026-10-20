package org.sitenetsoft.quarkus.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Nothing listens on the configured port and no deadline is set: the call fails fast with UNAVAILABLE.
 */
public class RefusedConnectionTest {

    private static final int CLOSED_PORT = closedPort();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(CLOSED_PORT))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(CLOSED_PORT))
            .overrideConfigKey("quarkus.topaz.plain-text", "true");

    @Inject
    TopazClient client;

    @Test
    void checkFailsFastWithUnavailable() {
        long t0 = System.nanoTime();
        assertThatThrownBy(() -> client.directory().check("todo", "todo-1", "owner", "user", "rick@the-citadel.com")
                .await().atMost(Duration.ofSeconds(10)))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
                .isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isLessThan(Duration.ofSeconds(5));
    }

    private static int closedPort() {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
