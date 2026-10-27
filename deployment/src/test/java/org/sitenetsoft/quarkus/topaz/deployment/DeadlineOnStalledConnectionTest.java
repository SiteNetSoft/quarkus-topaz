package org.sitenetsoft.quarkus.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

/**
 * Topaz accepts connections and never answers. The client itself must end every call at the configured deadline.
 */
public class DeadlineOnStalledConnectionTest {

    private static final ServerSocket SILENT = SilentListener.open();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(SilentListener.class))
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.deadline", "1s");

    @Inject
    TopazClient client;

    @AfterAll
    static void close() throws IOException {
        SILENT.close();
    }

    @Test
    void directoryCheckFailsAtDeadline() {
        assertDeadlineExceeded(() -> client.directory().check("todo", "todo-1", "owner", "user", "rick@the-citadel.com")
                .await().atMost(Duration.ofSeconds(10)));
    }

    @Test
    void authorizerInfoFailsAtDeadline() {
        assertDeadlineExceeded(() -> client.authorizer().info().await().atMost(Duration.ofSeconds(10)));
    }

    @Test
    void manifestStreamFailsAtDeadline() {
        Uni<String> manifest = client.directory().getManifest();
        assertDeadlineExceeded(() -> manifest.await().atMost(Duration.ofSeconds(10)));
    }

    @Test
    void blockingFacadeFailsAtDeadline() {
        assertDeadlineExceeded(() -> client.blocking().directory().check("todo", "todo-1", "owner", "user",
                "rick@the-citadel.com"));
    }

    private static void assertDeadlineExceeded(Runnable call) {
        long t0 = System.nanoTime();
        assertThatThrownBy(call::run)
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
                .isEqualTo(Status.Code.DEADLINE_EXCEEDED);
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isBetween(Duration.ofSeconds(1), Duration.ofSeconds(3));
    }
}
