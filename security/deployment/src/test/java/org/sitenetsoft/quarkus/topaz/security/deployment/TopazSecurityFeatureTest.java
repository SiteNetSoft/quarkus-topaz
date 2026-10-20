package org.sitenetsoft.quarkus.topaz.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.runtime.TopazEnforcementConfig;

import io.quarkus.test.QuarkusUnitTest;

public class TopazSecurityFeatureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.port", "1")
            .overrideConfigKey("quarkus.topaz.directory.port", "1")
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .setLogRecordPredicate(r -> r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records)
                    .anyMatch(r -> r.getMessage().contains("topaz-security")
                            || (r.getParameters() != null && java.util.Arrays.toString(r.getParameters())
                                    .contains("topaz-security"))));

    @Inject
    TopazEnforcementConfig config;

    @Test
    void defaults() {
        assertThat(config.decision()).isEqualTo("allowed");
        assertThat(config.anonymous()).isEqualTo(TopazEnforcementConfig.Anonymous.DENY);
        assertThat(config.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.policyRoot()).isEmpty();
        assertThat(config.additionalDecisions()).isEmpty();
    }

    @Test
    void featureIsInstalled() {
        // the "Installed features" log assertion above is the check that the feature is registered
        assertThat(TopazSecurityProcessor.FEATURE).isNotBlank();
    }
}
