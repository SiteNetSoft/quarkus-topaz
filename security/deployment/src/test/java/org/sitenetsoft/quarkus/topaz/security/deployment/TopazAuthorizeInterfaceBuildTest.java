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
 * With the {@code @Path} on an interface, the implementing class is a resource; the build error for its helper
 * method must say that the method lacks an HTTP method annotation, not that the class is not a resource.
 */
public class TopazAuthorizeInterfaceBuildTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(XApi.class, XImpl.class))
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .assertException(t -> assertThat(t).hasMessageContaining("@TopazAuthorize")
                    .hasMessageContaining(XImpl.class.getName())
                    .hasMessageContaining("HTTP method annotation")
                    .hasMessageNotContaining("can only be derived on a JAX-RS resource"));

    @Test
    void buildFails() {
        fail("should not build");
    }

    @Path("/x")
    public interface XApi {

        @GET
        String get();
    }

    public static class XImpl implements XApi {

        @Override
        public String get() {
            return "x";
        }

        @TopazAuthorize
        public String helper() {
            return "helper";
        }
    }
}
