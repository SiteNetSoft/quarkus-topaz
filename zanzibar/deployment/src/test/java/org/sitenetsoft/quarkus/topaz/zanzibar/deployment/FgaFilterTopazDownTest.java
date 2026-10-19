package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Topaz accepts connections and never answers; the call must end in a 5xx within quarkus-zanzibar's filter timeout, never in
 * a permit.
 */
public class FgaFilterTopazDownTest {

    // Accepted connections queue in the backlog and are never read, so the HTTP/2 handshake never completes.
    private static final ServerSocket SILENT = silentListener();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(FgaTodoResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            // the test identities carry a single role, which the default extractor would mistake for a user type
            .overrideConfigKey("quarkus.zanzibar.extract-user-type-from-roles", "false")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.deadline", "1s");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @AfterAll
    static void close() throws IOException {
        SILENT.close();
    }

    @Test
    void neverPermitsWhenTopazIsDown() {
        long t0 = System.nanoTime();
        int status = given().auth().preemptive().basic("rick@the-citadel.com", "rick").get("/fga/todos/todo-1").then()
                .extract().statusCode();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
        // quarkus-zanzibar maps a failed check to InternalServerErrorException
        assertThat(status).isIn(500, 503);
        // Observed: against a socket that never completes the HTTP/2 handshake the 1s gRPC deadline does not end the call;
        // quarkus-zanzibar's own 5s filter timeout does (about 5.3s). The call is still bounded and never permitted.
        assertThat(elapsed).isLessThan(Duration.ofSeconds(10));
    }

    private static ServerSocket silentListener() {
        try {
            return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
