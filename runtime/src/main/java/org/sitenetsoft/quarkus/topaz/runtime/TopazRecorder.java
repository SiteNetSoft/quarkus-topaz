package org.sitenetsoft.quarkus.topaz.runtime;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.sitenetsoft.quarkus.topaz.AuthorizerClient;
import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.MethodDescriptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.grpc.runtime.GrpcClientInterceptorContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;

@Recorder
public class TopazRecorder {

    private final RuntimeValue<TopazConfig> runtimeConfig;

    public TopazRecorder(RuntimeValue<TopazConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public RuntimeValue<TopazClient> createClient(RuntimeValue<Vertx> vertx, Supplier<TlsConfigurationRegistry> tls,
            ShutdownContext shutdown) {
        TopazConfig config = runtimeConfig.getValue();
        ResolvedServiceSettings authorizer = ResolvedServiceSettings.resolve(config, config.authorizer(), 8282);
        ResolvedServiceSettings directory = ResolvedServiceSettings.resolve(config, config.directory(), 9292);
        TlsConfigurationRegistry registry = tls.get();
        Optional<TlsConfiguration> authorizerTls = resolveTls(authorizer, registry);
        Optional<TlsConfiguration> directoryTls = resolveTls(directory, registry);

        TopazClient.Builder builder = TopazClient.builder()
                .settings(authorizer, authorizerTls, directory, directoryTls)
                .vertx(vertx.getValue())
                .interceptors(List.of(new LazyGlobalInterceptors()));
        config.tenantId().ifPresent(builder::tenantId);
        TopazClient client = builder.build();
        shutdown.addShutdownTask(client::close);
        return new RuntimeValue<>(client);
    }

    public RuntimeValue<AuthorizerClient> authorizer(RuntimeValue<TopazClient> client) {
        return new RuntimeValue<>(client.getValue().authorizer());
    }

    public RuntimeValue<DirectoryClient> directory(RuntimeValue<TopazClient> client) {
        return new RuntimeValue<>(client.getValue().directory());
    }

    private static Optional<TlsConfiguration> resolveTls(ResolvedServiceSettings s, TlsConfigurationRegistry registry) {
        if (s.plainText().orElse(false)) {
            return Optional.empty();
        }
        Optional<TlsConfiguration> named = Optional.empty();
        if (s.tlsConfigurationName().isPresent()) {
            String name = s.tlsConfigurationName().get();
            named = registry.get(name);
            if (named.isEmpty()) {
                throw new IllegalStateException("TLS configuration '" + name + "' for Topaz service " + s.host()
                        + ":" + s.port() + " was not found");
            }
        }
        if (named.isPresent()) {
            return named;
        }
        // The registry default is used only if it is usable (trust store or trust-all); otherwise TLS, when
        // required by plain-text=false, falls back to the JVM default trust in TopazChannels.
        Optional<TlsConfiguration> def = registry.getDefault();
        return usable(def) ? def : Optional.empty();
    }

    private static boolean usable(Optional<TlsConfiguration> def) {
        return def.isPresent() && (def.get().getTrustStoreOptions() != null || def.get().isTrustAll());
    }

    /**
     * Applies Quarkus gRPC's global client interceptors. They are resolved on first call because the interceptor
     * storage bean is not initialized yet while this recorder runs.
     */
    static final class LazyGlobalInterceptors implements ClientInterceptor {
        private volatile List<ClientInterceptor> resolved;

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions options, Channel next) {
            List<ClientInterceptor> list = resolved;
            if (list == null) {
                InstanceHandle<GrpcClientInterceptorContainer> container = Arc.container()
                        .instance(GrpcClientInterceptorContainer.class);
                if (container.isAvailable()) {
                    list = container.get().getSortedGlobalInterceptors();
                    resolved = list;
                } else {
                    // Not available yet: no global interceptors for this call, retry on the next one.
                    list = List.of();
                }
            }
            return list.isEmpty() ? next.newCall(method, options)
                    : ClientInterceptors.intercept(next, list).newCall(method, options);
        }
    }
}
