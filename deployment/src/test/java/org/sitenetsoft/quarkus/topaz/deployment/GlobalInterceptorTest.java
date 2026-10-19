package org.sitenetsoft.quarkus.topaz.deployment;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Global gRPC client interceptors (the hook used by quarkus-opentelemetry and Micrometer) must apply to the Topaz
 * channels.
 */
public class GlobalInterceptorTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("topaz").addClass(RecordingInterceptor.class))
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data");

    @Inject
    TopazClient topaz;

    @Test
    void globalInterceptorSeesTopazCalls() {
        // Negative control: nothing is recorded until a call goes through a Topaz channel
        RecordingInterceptor.CALLS.clear();
        assertThat(RecordingInterceptor.CALLS).isEmpty();

        topaz.authorizer().info().await().atMost(ofSeconds(10));
        topaz.directory().getManifest().await().atMost(ofSeconds(10));

        assertThat(RecordingInterceptor.CALLS).contains("aserto.authorizer.v2.Authorizer/Info",
                "aserto.directory.model.v3.Model/GetManifest");
    }

    @GlobalInterceptor
    @ApplicationScoped
    public static class RecordingInterceptor implements ClientInterceptor {

        static final CopyOnWriteArrayList<String> CALLS = new CopyOnWriteArrayList<>();

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions, Channel next) {
            CALLS.add(method.getFullMethodName());
            return next.newCall(method, callOptions);
        }
    }
}
