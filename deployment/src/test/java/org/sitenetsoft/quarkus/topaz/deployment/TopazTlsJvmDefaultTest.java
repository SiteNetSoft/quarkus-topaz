package org.sitenetsoft.quarkus.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.StatusRuntimeException;
import io.quarkus.test.QuarkusUnitTest;

/** {@code plain-text=false} with no TLS configuration starts (TLS with JVM default trust) instead of failing. */
public class TopazTlsJvmDefaultTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.plain-text", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.port", "1")
            .overrideConfigKey("quarkus.topaz.directory.port", "1")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false");

    @Inject
    TopazClient topaz;

    @Test
    void startsAndCallsFailWithStatus() {
        Throwable t = catchThrowable(() -> topaz.blocking().authorizer().info());
        assertThat(t).isInstanceOf(StatusRuntimeException.class);
    }
}
