package org.sitenetsoft.quarkus.topaz.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * Factory of Vert.x gRPC channels for Topaz services.
 */
public final class TopazChannels {

    private TopazChannels() {
    }

    /**
     * Create a channel.
     * {@code plainText=true} always wins.
     * It produces a plaintext channel and ignores {@code tls}.
     * A named TLS configuration that was not resolved fails instead of silently downgrading.
     * If TLS is forced with {@code plainText=false} and no TLS configuration was resolved, the channel uses TLS with
     * the JVM default trust.
     * The channel never falls back to plaintext when TLS is required.
     *
     * @param vertx the Vert.x instance
     * @param s the settings of the service
     * @param tls the resolved TLS configuration, if any
     * @param interceptors the interceptors to apply to the channel
     * @return a holder of the channel and its client
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

    /**
     * Compute the HTTP/2 transport options for {@link #create}.
     * Tests use it.
     *
     * @param s the settings of the service
     * @param tls the resolved TLS configuration, if any
     * @return the transport options
     */
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
            // plain-text=false without a usable TLS configuration uses TLS with the JVM default trust.
            http.setSsl(true).setUseAlpn(true);
        } else {
            http.setSsl(false).setHttp2ClearTextUpgrade(false);
        }
        return http;
    }

    /**
     * Holder of a channel and the gRPC client that owns it.
     *
     * @param channel the channel
     * @param client the gRPC client behind the channel
     * @param closed whether the holder is closed
     */
    public record ManagedChannelHolder(Channel channel, GrpcClient client, AtomicBoolean closed)
            implements AutoCloseable {

        /**
         * Create an open holder.
         *
         * @param channel the channel
         * @param client the gRPC client behind the channel
         */
        public ManagedChannelHolder(Channel channel, GrpcClient client) {
            this(channel, client, new AtomicBoolean());
        }

        /** Close the gRPC client and mark the holder closed. */
        @Override
        public void close() {
            closed.set(true);
            client.close();
        }

        /**
         * Check whether {@link #close()} has been called.
         *
         * @return {@code true} once the holder is closed
         */
        public boolean isClosed() {
            return closed.get();
        }
    }
}
