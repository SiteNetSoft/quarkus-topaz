package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sitenetsoft.quarkus.topaz.IdentityContexts.subject;

import java.util.Map;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesTopazTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            // classpath: locations resolve against the application, like src/main/resources in an app
            .withApplicationRoot(jar -> jar.addAsResource("topaz"))
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data");

    @Inject
    TopazClient topaz;

    @Test
    void authorizerServes() {
        assertThat(topaz.authorizer().info().await().atMost(ofSeconds(10)).getVersion()).isNotBlank();
    }

    @Test
    void manifestLoaded() {
        assertThat(topaz.directory().getManifest().await().atMost(ofSeconds(10))).contains("can_delete: owner");
    }

    @Test
    void dataImported() {
        assertThat(topaz.directory().check("todo", "todo-1", "owner", "user", "rick@the-citadel.com")
                .await().atMost(ofSeconds(10))).isTrue();
        assertThat(topaz.directory().check("todo", "todo-1", "owner", "user", "morty@the-citadel.com")
                .await().atMost(ofSeconds(10))).isFalse();
    }

    @Test
    void policyEvaluates() {
        assertThat(topaz.authorizer().isAllowed(subject("rick@the-citadel.com"), "todoApp.GET.todos", "allowed",
                Map.of()).await().atMost(ofSeconds(10))).isTrue();
        assertThat(topaz.authorizer().isAllowed(subject("rick@the-citadel.com"), "todoApp.DELETE.todos.__id",
                "allowed", Map.of("id", "todo-1")).await().atMost(ofSeconds(10))).isTrue();
        assertThat(topaz.authorizer().isAllowed(subject("morty@the-citadel.com"), "todoApp.DELETE.todos.__id",
                "allowed", Map.of("id", "todo-1")).await().atMost(ofSeconds(10))).isFalse();
    }

    @Test
    void unknownPolicyPathPropagatesError() {
        // The error must come from Topaz (INVALID_ARGUMENT E30010 on 0.33.22), not from failing to reach it
        assertThatThrownBy(() -> topaz.authorizer().isAllowed(subject("rick@the-citadel.com"), "todoApp.NOPE",
                "allowed", Map.of()).await().atMost(ofSeconds(10)))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
                .isNotIn(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED);
    }

    @Test
    void consoleUrlInjected() {
        assertThat(ConfigProvider.getConfig().getOptionalValue("quarkus.topaz.console-url", String.class))
                .hasValueSatisfying(url -> assertThat(url).matches("^http://[^:]+:\\d+$"));
    }
}
