package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A blocking {@code @TopazAuthorize} method called on the event loop is refused rather than blocking it.
 */
public class TopazAuthorizeEventLoopTest {

    private static final String EVENT_LOOP_TEXT = "@TopazAuthorize/@TopazCheck on a blocking method cannot run on the event";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(EventLoopRoutes.class, SecuredService.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> {
                for (Throwable t = r.getThrown(); t != null; t = t.getCause()) {
                    if (t.getMessage() != null && t.getMessage().startsWith(EVENT_LOOP_TEXT)) {
                        return true;
                    }
                }
                return false;
            }));

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void eventLoopCallerIsRejected() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/loop/blocking").then().statusCode(500);
    }
}
