package org.sitenetsoft.quarkus.topaz.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TopazAuthorizeNoPathBuildTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(NotAResource.class))
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .assertException(t -> assertThat(t).hasMessageContaining("@TopazAuthorize")
                    .hasMessageContaining("path")
                    .hasMessageContaining(NotAResource.class.getName()));

    @Test
    void buildFails() {
        fail("should not build");
    }
}
