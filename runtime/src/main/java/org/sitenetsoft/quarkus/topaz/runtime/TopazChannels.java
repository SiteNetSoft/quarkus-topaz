package org.sitenetsoft.quarkus.topaz.runtime;

import java.util.List;
import java.util.Optional;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;
import io.vertx.grpc.client.GrpcClientOptions;

/**
 * Builds Vert.x gRPC channels for Topaz services.
 */
public final class TopazChannels {

    private TopazChannels() {
    }

    public static boolean useTls(ResolvedServiceSettings s, Optional<TlsConfiguration> named,
            Optional<TlsConfiguration> registryDefault) {
        if (s.plainText().isPresent()) {
            return !s.plainText().get();
        }
        if (named.isPresent()) {
            return true;
        }
        return registryDefault.isPresent()
                && (registryDefault.get().getTrustStoreOptions() != null || registryDefault.get().isTrustAll());
    }

    /**
     * Creates a channel. {@code plainText=true} always wins: a plaintext channel is produced and {@code tls} is
     * ignored. A named TLS configuration that was not resolved fails instead of silently downgrading. If TLS is
     * forced ({@code plainText=false}) and no TLS configuration was resolved, TLS is used with the JVM default trust.
     * Never falls back to plaintext when TLS is required.
     */
    public static ManagedChannelHolder create(Vertx vertx, ResolvedServiceSettings s, Optional<TlsConfiguration> tls,
            List<ClientInterceptor> interceptors) {
        HttpClientOptions http = transportOptions(s, tls);
        GrpcClient client = GrpcClient.client(vertx, new GrpcClientOptions().setTransportOptions(http));
        Channel channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(s.port(), s.host()));
        if (!interceptors.isEmpty()) {
            channel = ClientInterceptors.intercept(channel, interceptors);
        }
        return new ManagedChannelHolder(channel, client);
    }

    /** HTTP/2 transport options for {@link #create}; exposed for tests. */
    static HttpClientOptions transportOptions(ResolvedServiceSettings s, Optional<TlsConfiguration> tls) {
        HttpClientOptions http = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        if (s.plainText().orElse(false)) {
            return http.setSsl(false).setHttp2ClearTextUpgrade(false);
        }
        if (tls.isEmpty() && s.tlsConfigurationName().isPresent()) {
            throw new IllegalStateException("TLS configuration '" + s.tlsConfigurationName().get()
                    + "' for Topaz service " + s.host() + ":" + s.port() + " was not found");
        }
        if (tls.isPresent()) {
            http.setSsl(true).setUseAlpn(true);
            TlsConfigUtils.configure(http, tls.get());
        } else if (s.plainText().isPresent()) {
            // plain-text=false without a usable TLS configuration: TLS with the JVM default trust.
            http.setSsl(true).setUseAlpn(true);
        } else {
            http.setSsl(false).setHttp2ClearTextUpgrade(false);
        }
        return http;
    }

    public record ManagedChannelHolder(Channel channel, GrpcClient client) implements AutoCloseable {
        @Override
        public void close() {
            client.close();
        }
    }
}
