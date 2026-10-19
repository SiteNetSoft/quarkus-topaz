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
     * ignored. Otherwise, if TLS is forced ({@code plainText=false}) or a named TLS configuration is set but no
     * TLS configuration was resolved, this fails instead of silently downgrading to plaintext.
     */
    public static ManagedChannelHolder create(Vertx vertx, ResolvedServiceSettings s, Optional<TlsConfiguration> tls,
            List<ClientInterceptor> interceptors) {
        boolean plain = s.plainText().orElse(false);
        if (plain) {
            tls = Optional.empty();
        } else if (tls.isEmpty()) {
            if (s.plainText().isPresent()) {
                throw new IllegalStateException("TLS was required for Topaz service " + s.host() + ":" + s.port()
                        + " (plain-text=false) but no TLS configuration resolved");
            }
            if (s.tlsConfigurationName().isPresent()) {
                throw new IllegalStateException("TLS configuration '" + s.tlsConfigurationName().get()
                        + "' for Topaz service " + s.host() + ":" + s.port() + " was not found");
            }
        }
        HttpClientOptions http = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        if (tls.isPresent()) {
            http.setSsl(true).setUseAlpn(true);
            TlsConfigUtils.configure(http, tls.get());
        } else {
            http.setSsl(false).setHttp2ClearTextUpgrade(false);
        }
        GrpcClient client = GrpcClient.client(vertx, new GrpcClientOptions().setTransportOptions(http));
        Channel channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(s.port(), s.host()));
        if (!interceptors.isEmpty()) {
            channel = ClientInterceptors.intercept(channel, interceptors);
        }
        return new ManagedChannelHolder(channel, client);
    }

    public record ManagedChannelHolder(Channel channel, GrpcClient client) implements AutoCloseable {
        @Override
        public void close() {
            client.close();
        }
    }
}
