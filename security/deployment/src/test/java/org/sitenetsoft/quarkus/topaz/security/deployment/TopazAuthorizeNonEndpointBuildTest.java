package org.sitenetsoft.quarkus.topaz.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;

import io.quarkus.test.QuarkusUnitTest;

/**
 * A derived path means the policy of the HTTP request being served, so a plain method of a resource class may not
 * omit it.
 */
public class TopazAuthorizeNonEndpointBuildTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ResourceWithHelper.class))
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .assertException(t -> assertThat(t).hasMessageContaining("@TopazAuthorize")
                    .hasMessageContaining("helper")
                    .hasMessageContaining("set path explicitly"));

    @Test
    void buildFails() {
        fail("should not build");
    }

    @Path("/x")
    public static class ResourceWithHelper {

        @GET
        public String get() {
            return "x";
        }

        @TopazAuthorize
        public String helper() {
            return "helper";
        }
    }
}
