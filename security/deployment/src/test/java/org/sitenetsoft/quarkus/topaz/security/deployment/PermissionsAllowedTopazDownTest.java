package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * The authorizer accepts connections and never answers, and no gRPC deadline is set: only the enforcement timeout can
 * end a blocking {@code @PermissionsAllowed} call through the bridge.
 */
public class PermissionsAllowedTopazDownTest {

    // Accepted connections queue in the backlog and are never read, so the HTTP/2 handshake never completes.
    private static final ServerSocket SILENT = silentListener();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PermissionsResource.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "1s")
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.enabled", "true")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.WARNING.intValue()
                    && r.getLoggerName().endsWith("TopazPermissionAugmentor"))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> r.getMessage().contains("permission check timed out")));

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @AfterAll
    static void close() throws IOException {
        SILENT.close();
    }

    @Test
    void permissionCheckTimesOutTo403() {
        long t0 = System.nanoTime();
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/perm/list").then().statusCode(403);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(4));
    }

    private static ServerSocket silentListener() {
        try {
            return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
