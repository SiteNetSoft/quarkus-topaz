package org.sitenetsoft.quarkus.topaz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sitenetsoft.quarkus.topaz.runtime.ResolvedServiceSettings;
import org.sitenetsoft.quarkus.topaz.runtime.TopazChannels;
import org.sitenetsoft.quarkus.topaz.runtime.TopazChannels.ManagedChannelHolder;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.Vertx;

/**
 * Entry point of the Topaz client API: reactive {@link #authorizer()} and {@link #directory()} clients and a
 * {@link #blocking()} view of both.
 */
public final class TopazClient implements AutoCloseable {

    private final AuthorizerClient authorizer;
    private final DirectoryClient directory;
    private final BlockingTopazClient blocking;
    private final List<ManagedChannelHolder> holders;
    private final Vertx ownedVertx;
    private final AtomicBoolean closed = new AtomicBoolean();

    private TopazClient(AuthorizerClient authorizer, DirectoryClient directory, List<ManagedChannelHolder> holders,
            Vertx ownedVertx) {
        this.authorizer = authorizer;
        this.directory = directory;
        this.blocking = new BlockingTopazClient(authorizer, directory);
        this.holders = holders;
        this.ownedVertx = ownedVertx;
    }

    /** Creates a builder with defaults: authorizer {@code localhost:8282}, directory {@code localhost:9292}. */
    public static Builder builder() {
        return new Builder();
    }

    /** The reactive authorizer client. */
    public AuthorizerClient authorizer() {
        return authorizer;
    }

    /** The reactive directory client. */
    public DirectoryClient directory() {
        return directory;
    }

    /** A blocking view of the authorizer and directory clients. */
    public BlockingTopazClient blocking() {
        return blocking;
    }

    /**
     * Closes the channels this client created, and the Vert.x instance if the builder created it. Channels passed
     * to {@link Builder#authorizerChannel} / {@link Builder#directoryChannel} belong to the caller and are not
     * closed. Idempotent.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closeAll(holders, ownedVertx);
        }
    }

    /** The channel holders this client owns; exposed for tests. */
    List<ManagedChannelHolder> holders() {
        return holders;
    }

    private static void closeAll(List<ManagedChannelHolder> holders, Vertx ownedVertx) {
        for (ManagedChannelHolder h : holders) {
            h.close();
        }
        if (ownedVertx != null) {
            ownedVertx.close();
        }
    }

    public static final class Builder {

        private ResolvedServiceSettings authorizerSettings = defaults("localhost", 8282);
        private ResolvedServiceSettings directorySettings = defaults("localhost", 9292);
        private Optional<TlsConfiguration> authorizerTls = Optional.empty();
        private Optional<TlsConfiguration> directoryTls = Optional.empty();
        private Optional<String> tenantId = Optional.empty();
        private Vertx vertx;
        private List<ClientInterceptor> interceptors = List.of();
        private Channel authorizerChannel;
        private Channel directoryChannel;

        private Builder() {
        }

        private static ResolvedServiceSettings defaults(String host, int port) {
            return new ResolvedServiceSettings(host, port, Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty());
        }

        private static ResolvedServiceSettings withEndpoint(ResolvedServiceSettings s, String host, int port) {
            return new ResolvedServiceSettings(Objects.requireNonNull(host, "host"), port, s.apiKey(),
                    s.tlsConfigurationName(), s.plainText(), s.deadline());
        }

        private static ResolvedServiceSettings withApiKey(ResolvedServiceSettings s, Optional<String> apiKey) {
            return new ResolvedServiceSettings(s.host(), s.port(), apiKey, s.tlsConfigurationName(), s.plainText(),
                    s.deadline());
        }

        private static ResolvedServiceSettings withPlainText(ResolvedServiceSettings s, Optional<Boolean> plainText) {
            return new ResolvedServiceSettings(s.host(), s.port(), s.apiKey(), s.tlsConfigurationName(), plainText,
                    s.deadline());
        }

        private static ResolvedServiceSettings withDeadline(ResolvedServiceSettings s, Optional<Duration> deadline) {
            return new ResolvedServiceSettings(s.host(), s.port(), s.apiKey(), s.tlsConfigurationName(),
                    s.plainText(), deadline);
        }

        /** Sets the authorizer endpoint. */
        public Builder authorizer(String host, int port) {
            authorizerSettings = withEndpoint(authorizerSettings, host, port);
            return this;
        }

        /** Sets the directory endpoint. */
        public Builder directory(String host, int port) {
            directorySettings = withEndpoint(directorySettings, host, port);
            return this;
        }

        /** Applies to both services. */
        public Builder plainText(boolean v) {
            authorizerSettings = withPlainText(authorizerSettings, Optional.of(v));
            directorySettings = withPlainText(directorySettings, Optional.of(v));
            return this;
        }

        /** Applies to both services; sent as {@code authorization: basic <key>}. */
        public Builder apiKey(String k) {
            authorizerSettings = withApiKey(authorizerSettings, Optional.ofNullable(k));
            directorySettings = withApiKey(directorySettings, Optional.ofNullable(k));
            return this;
        }

        /** Sent as {@code aserto-tenant-id} together with the API key. */
        public Builder tenantId(String t) {
            tenantId = Optional.ofNullable(t);
            return this;
        }

        /** Per-call deadline for both services; also bounds the blocking facade's wait. */
        public Builder deadline(Duration d) {
            authorizerSettings = withDeadline(authorizerSettings, Optional.ofNullable(d));
            directorySettings = withDeadline(directorySettings, Optional.ofNullable(d));
            return this;
        }

        /** TLS configuration for both services. */
        public Builder tlsConfiguration(TlsConfiguration tls) {
            this.authorizerTls = Optional.ofNullable(tls);
            this.directoryTls = this.authorizerTls;
            return this;
        }

        /** Vert.x instance to create channels with; not closed by {@link TopazClient#close()}. */
        public Builder vertx(Vertx vertx) {
            this.vertx = vertx;
            return this;
        }

        /** Extra interceptors, applied after the API key interceptor. */
        public Builder interceptors(List<ClientInterceptor> extra) {
            this.interceptors = List.copyOf(extra);
            return this;
        }

        /** Uses this channel for the authorizer instead of creating one; the caller keeps ownership. */
        public Builder authorizerChannel(Channel ch) {
            this.authorizerChannel = ch;
            return this;
        }

        /** Uses this channel for the directory instead of creating one; the caller keeps ownership. */
        public Builder directoryChannel(Channel ch) {
            this.directoryChannel = ch;
            return this;
        }

        /**
         * Internal: used by the extension's recorder. Not API. Replaces both services' settings and their TLS
         * configurations at once.
         */
        public Builder settings(ResolvedServiceSettings authorizer, Optional<TlsConfiguration> authorizerTls,
                ResolvedServiceSettings directory, Optional<TlsConfiguration> directoryTls) {
            this.authorizerSettings = Objects.requireNonNull(authorizer, "authorizer");
            this.authorizerTls = Objects.requireNonNull(authorizerTls, "authorizerTls");
            this.directorySettings = Objects.requireNonNull(directory, "directory");
            this.directoryTls = Objects.requireNonNull(directoryTls, "directoryTls");
            return this;
        }

        /** Builds the client. Channels created here are closed by {@link TopazClient#close()}. */
        public TopazClient build() {
            List<ManagedChannelHolder> holders = new ArrayList<>();
            Vertx owned = null;
            try {
                Vertx v = vertx;
                if (v == null && (authorizerChannel == null || directoryChannel == null)) {
                    owned = Vertx.vertx();
                    v = owned;
                }
                Channel authz = channel(authorizerChannel, authorizerSettings, authorizerTls, v, holders);
                Channel dir = channel(directoryChannel, directorySettings, directoryTls, v, holders);
                return new TopazClient(new AuthorizerClient(authz, authorizerSettings.deadline()),
                        new DirectoryClient(dir, directorySettings.deadline()), List.copyOf(holders), owned);
            } catch (RuntimeException e) {
                closeAll(holders, owned);
                throw e;
            }
        }

        private Channel channel(Channel given, ResolvedServiceSettings s, Optional<TlsConfiguration> tls, Vertx v,
                List<ManagedChannelHolder> holders) {
            List<ClientInterceptor> all = new ArrayList<>();
            s.apiKey().ifPresent(k -> all.add(new ApiKeyClientInterceptor(k, tenantId)));
            all.addAll(interceptors);
            if (given != null) {
                return all.isEmpty() ? given : ClientInterceptors.intercept(given, all);
            }
            ManagedChannelHolder h = TopazChannels.create(v, s, tls, all);
            holders.add(h);
            return h.channel();
        }
    }
}
