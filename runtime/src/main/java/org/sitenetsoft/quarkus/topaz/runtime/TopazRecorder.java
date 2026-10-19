package org.sitenetsoft.quarkus.topaz.runtime;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.sitenetsoft.quarkus.topaz.AuthorizerClient;
import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.TopazClient;

import io.grpc.ClientInterceptor;
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

    /**
     * Returns a memoizing supplier: the client is built on first use, because the Vert.x bean is not yet
     * initialized while RUNTIME_INIT recorders run.
     */
    public Supplier<TopazClient> createClient(Supplier<TlsConfigurationRegistry> tls, ShutdownContext shutdown) {
        return new Supplier<>() {
            private volatile TopazClient instance;

            @Override
            public TopazClient get() {
                TopazClient c = instance;
                if (c == null) {
                    synchronized (this) {
                        c = instance;
                        if (c == null) {
                            c = build(tls, shutdown);
                            instance = c;
                        }
                    }
                }
                return c;
            }
        };
    }

    private TopazClient build(Supplier<TlsConfigurationRegistry> tls, ShutdownContext shutdown) {
        TopazConfig config = runtimeConfig.getValue();
        ResolvedServiceSettings authorizer = ResolvedServiceSettings.resolve(config, config.authorizer(), 8282);
        ResolvedServiceSettings directory = ResolvedServiceSettings.resolve(config, config.directory(), 9292);
        TlsConfigurationRegistry registry = tls.get();

        // TopazClient.Builder.settings takes a single TlsConfiguration for both services. Use the authorizer's;
        // fall back to the directory's when the authorizer does not use TLS.
        Optional<TlsConfiguration> authorizerTls = resolveTls(authorizer, registry);
        Optional<TlsConfiguration> directoryTls = resolveTls(directory, registry);
        Optional<TlsConfiguration> chosen = authorizerTls.isPresent() ? authorizerTls : directoryTls;

        Vertx vertx = Arc.container().instance(Vertx.class).get();
        InstanceHandle<GrpcClientInterceptorContainer> container = Arc.container()
                .instance(GrpcClientInterceptorContainer.class);
        List<ClientInterceptor> interceptors = container.isAvailable()
                ? container.get().getSortedGlobalInterceptors()
                : List.of();

        TopazClient.Builder builder = TopazClient.builder()
                .settings(authorizer, directory, chosen)
                .vertx(vertx)
                .interceptors(interceptors);
        config.tenantId().ifPresent(builder::tenantId);
        TopazClient client = builder.build();
        shutdown.addShutdownTask(client::close);
        return client;
    }

    public Supplier<AuthorizerClient> authorizer(Supplier<TopazClient> client) {
        return () -> client.get().authorizer();
    }

    public Supplier<DirectoryClient> directory(Supplier<TopazClient> client) {
        return () -> client.get().directory();
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
        Optional<TlsConfiguration> def = registry.getDefault();
        if (TopazChannels.useTls(s, named, def)) {
            return named.isPresent() ? named : def;
        }
        return Optional.empty();
    }
}
