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
 * Topaz accepts connections and never answers. The client-wide deadline (1s) is shorter than the quarkus-zanzibar filter
 * timeout (3s), so the client ends the call with DEADLINE_EXCEEDED and the request fails with a 5xx, never a permit.
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
            .overrideConfigKey("quarkus.topaz.deadline", "1s")
            .overrideConfigKey("quarkus.zanzibar.filter.timeout", "3s")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.SEVERE.intValue()
                    && r.getLoggerName().contains("Zanzibar"))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> mentionsDeadline(String.valueOf(r.getMessage()))
                    || (r.getThrown() != null && mentionsDeadline(r.getThrown().toString()))
                    || mentionsDeadline(Arrays.toString(r.getParameters()))));

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
        // the 1s client deadline, not the 3s filter timeout, ends the call (the log-record assertion confirms the cause)
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofSeconds(1)).isLessThan(Duration.ofSeconds(4));
    }

    private static boolean mentionsDeadline(String text) {
        return text.contains("DEADLINE_EXCEEDED") || text.contains("TimeoutException");
    }

    private static ServerSocket silentListener() {
        try {
            return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
