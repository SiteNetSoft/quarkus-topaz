package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.zanzibar.runtime.TopazZanzibarConfig;

import io.quarkus.test.QuarkusUnitTest;

public class TopazZanzibarFeatureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.port", "1")
            .overrideConfigKey("quarkus.topaz.directory.port", "1")
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .setLogRecordPredicate(r -> r.getMessage() != null && r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> {
                String message = String.format(r.getMessage(), r.getParameters());
                return message.contains("topaz-zanzibar") && message.contains("topaz") && message.contains("zanzibar");
            }));

    @Inject
    TopazZanzibarConfig config;

    @Test
    void defaultUserType() {
        assertThat(config.defaultUserType()).isEqualTo("user");
    }

    @Test
    void featureIsInstalled() {
        // asserted on the "Installed features" log line by the extension's assertLogRecords above
        assertThat(TopazZanzibarProcessor.FEATURE).isNotBlank();
    }

    @Test
    void zanzibarCoreIsOnTheClasspath() {
        // the connector is useless without quarkus-zanzibar; a wrong artifactId in the pom would still compile this module
        assertThat(io.quarkiverse.zanzibar.RelationshipManager.class).isInterface();
    }
}
