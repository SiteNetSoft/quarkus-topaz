package org.sitenetsoft.quarkus.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TopazTlsMissingTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.tls-configuration-name", "nope")
            .assertException(t -> {
                Throwable c = t;
                StringBuilder all = new StringBuilder();
                while (c != null) {
                    all.append(c).append('\n');
                    c = c.getCause();
                }
                assertThat(all.toString()).contains("TLS configuration 'nope'");
            });

    @Test
    void failsFast() {
    }
}
