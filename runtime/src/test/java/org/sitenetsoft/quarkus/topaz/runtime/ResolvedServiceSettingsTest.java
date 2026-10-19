package org.sitenetsoft.quarkus.topaz.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

class ResolvedServiceSettingsTest {

    private static TopazConfig cfg(Map<String, String> props) {
        return new SmallRyeConfigBuilder()
                .withMapping(TopazConfig.class)
                .withSources(new PropertiesConfigSource(props, "test", 100))
                .build()
                .getConfigMapping(TopazConfig.class);
    }

    @Test
    void defaultsPerService() {
        var s = ResolvedServiceSettings.resolve(cfg(Map.of()), cfg(Map.of()).authorizer(), 8282);
        assertThat(s.host()).isEqualTo("localhost");
        assertThat(s.port()).isEqualTo(8282);
        assertThat(s.plainText()).isEmpty();
    }

    @Test
    void serviceOverridesRoot() {
        var c = cfg(Map.of("quarkus.topaz.api-key", "root", "quarkus.topaz.directory.api-key", "dir",
                "quarkus.topaz.directory.port", "19292"));
        assertThat(ResolvedServiceSettings.resolve(c, c.directory(), 9292))
                .extracting("apiKey", "port").containsExactly(Optional.of("dir"), 19292);
        assertThat(ResolvedServiceSettings.resolve(c, c.authorizer(), 8282).apiKey()).contains("root");
    }
}
