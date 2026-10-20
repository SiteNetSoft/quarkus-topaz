package org.sitenetsoft.quarkus.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Topaz accepts connections and never answers, and no deadline is configured: the health check gives up after 10s.
 * (A separate class from {@link TopazHealthDownTest} because the configuration differs.)
 */
public class TopazHealthStalledTest {

    // Accepted connections queue in the backlog and are never read, so the HTTP/2 handshake never completes.
    private static final ServerSocket SILENT = silentListener();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true");

    @AfterAll
    static void close() throws IOException {
        SILENT.close();
    }

    @Test
    void stalledTopazIsDownWithinTenSeconds() {
        long t0 = System.nanoTime();
        RestAssured.get("/q/health/ready").then().statusCode(503)
                .body("checks.find { it.name == 'Topaz' }.status", equalTo("DOWN"));
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isBetween(Duration.ofSeconds(10), Duration.ofSeconds(15));
    }

    private static ServerSocket silentListener() {
        try {
            return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
