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

/**
 * Recorder that creates the Topaz client beans at run time.
 */
@Recorder
public class TopazRecorder {

    private final RuntimeValue<TopazConfig> runtimeConfig;

    /**
     * Create the recorder.
     *
     * @param runtimeConfig the run-time configuration
     */
    public TopazRecorder(RuntimeValue<TopazConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Create the Topaz client and register its shutdown.
     *
     * @param vertx the Vert.x instance
     * @param tls supplier of the TLS configuration registry
     * @param shutdown the shutdown context that closes the client
     * @return the client
     */
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

    /**
     * Get the authorizer client of a Topaz client.
     *
     * @param client the Topaz client
     * @return the authorizer client
     */
    public RuntimeValue<AuthorizerClient> authorizer(RuntimeValue<TopazClient> client) {
        return new RuntimeValue<>(client.getValue().authorizer());
    }

    /**
     * Get the directory client of a Topaz client.
     *
     * @param client the Topaz client
     * @return the directory client
     */
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
        // Use the registry default only if it has a trust store or trusts all.
        // Otherwise TLS, if plain-text=false requires it, falls back to the JVM default trust in TopazChannels.
        Optional<TlsConfiguration> def = registry.getDefault();
        return usable(def) ? def : Optional.empty();
    }

    private static boolean usable(Optional<TlsConfiguration> def) {
        return def.isPresent() && (def.get().getTrustStoreOptions() != null || def.get().isTrustAll());
    }

    /**
     * Interceptor that applies the global client interceptors of Quarkus gRPC.
     * It resolves them on the first call, because the interceptor storage bean is not initialized while this
     * recorder runs.
     */
    static final class LazyGlobalInterceptors implements ClientInterceptor {
        private volatile List<ClientInterceptor> resolved;

        /** Create an interceptor that has not resolved the global interceptors yet. */
        LazyGlobalInterceptors() {
        }

        /** {@inheritDoc} */
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
                    // Not available yet. Apply no global interceptors to this call and retry on the next one.
                    list = List.of();
                }
            }
            return list.isEmpty() ? next.newCall(method, options)
                    : ClientInterceptors.intercept(next, list).newCall(method, options);
        }
    }
}
