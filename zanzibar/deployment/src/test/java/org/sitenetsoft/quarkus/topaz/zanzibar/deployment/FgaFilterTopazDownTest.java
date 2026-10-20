package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Topaz accepts connections and never answers. The quarkus-zanzibar filter timeout (set explicitly to 3s) ends the call
 * with a 5xx, never a permit. The client-wide gRPC deadline (1s) is configured but does not fire on a stalled connection.
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
            // DefaultUserExtractor resolves the subject type in a fixed order: the "type:id" split of the principal name
            // (extract-user-type-from-name), then the identity's single role (extract-user-type-from-roles, default true),
            // and the @FGAUserType("user") on FgaTodoResource only last, so the annotation does not stop role extraction.
            // TestIdentityController.add(user, password) without roles builds the role set {null}, so with role extraction
            // on Optional.of(null) throws an NPE (every request answers 500); in a real application a single role (say
            // "admin") would become the subject type and every check would answer 403. With it off, @FGAUserType supplies
            // "user". The test principals contain no ':', so the name split does not apply.
            .overrideConfigKey("quarkus.zanzibar.extract-user-type-from-roles", "false")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            // Configured, but observed not to end the call on a connection that never completes the HTTP/2 handshake
            // (tracked as a client finding); the filter timeout below is what bounds the request.
            .overrideConfigKey("quarkus.topaz.deadline", "1s")
            .overrideConfigKey("quarkus.zanzibar.filter.timeout", "3s")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.SEVERE.intValue()
                    && r.getLoggerName().contains("Zanzibar"))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> String.valueOf(r.getMessage()).contains("TimeoutException")
                    || (r.getThrown() != null && r.getThrown().toString().contains("TimeoutException"))
                    || Arrays.toString(r.getParameters()).contains("TimeoutException")));

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
        // the 3s filter timeout, not the 1s client deadline, ends the call (the log-record assertion confirms the cause)
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofSeconds(3)).isLessThan(Duration.ofSeconds(8));
    }

    private static ServerSocket silentListener() {
        try {
            return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
