package org.sitenetsoft.quarkus.topaz.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import java.util.List;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.KeyStoreOptions;

class TopazChannelsTest {

    private static ResolvedServiceSettings settings(Optional<Boolean> plainText, Optional<String> tlsName) {
        return new ResolvedServiceSettings("localhost", 8282, Optional.empty(), tlsName, plainText, Optional.empty());
    }

    private static TlsConfiguration withTrustStore() {
        TlsConfiguration c = mock(TlsConfiguration.class);
        when(c.getTrustStoreOptions()).thenReturn(new KeyStoreOptions());
        return c;
    }

    private static TlsConfiguration withoutTrust() {
        TlsConfiguration c = mock(TlsConfiguration.class);
        when(c.getTrustStoreOptions()).thenReturn(null);
        when(c.isTrustAll()).thenReturn(false);
        return c;
    }

    @Test
    void plainTextTrueWins() {
        assertThat(TopazChannels.useTls(settings(Optional.of(true), Optional.of("n")),
                Optional.of(withTrustStore()), Optional.of(withTrustStore()))).isFalse();
    }

    @Test
    void plainTextFalseForcesTls() {
        assertThat(TopazChannels.useTls(settings(Optional.of(false), Optional.empty()),
                Optional.empty(), Optional.empty())).isTrue();
    }

    @Test
    void namedConfigEnablesTls() {
        assertThat(TopazChannels.useTls(settings(Optional.empty(), Optional.of("n")),
                Optional.of(withoutTrust()), Optional.empty())).isTrue();
    }

    @Test
    void defaultWithTrustStoreEnablesTls() {
        assertThat(TopazChannels.useTls(settings(Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.of(withTrustStore()))).isTrue();
    }

    @Test
    void defaultWithoutTrustIsPlaintext() {
        assertThat(TopazChannels.useTls(settings(Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.of(withoutTrust()))).isFalse();
        assertThat(TopazChannels.useTls(settings(Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty())).isFalse();
    }

    @Test
    void createThrowsWhenTlsForcedButNoConfig() {
        assertThatThrownBy(() -> TopazChannels.create(mock(Vertx.class), settings(Optional.of(false), Optional.empty()),
                Optional.empty(), List.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("localhost:8282");
    }

    @Test
    void createThrowsWhenNamedTlsMissing() {
        assertThatThrownBy(() -> TopazChannels.create(mock(Vertx.class), settings(Optional.empty(), Optional.of("mytls")),
                Optional.empty(), List.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("mytls");
    }

    @Test
    void createPlainTextIgnoresTlsConfig() {
        Vertx vertx = Vertx.vertx();
        try (TopazChannels.ManagedChannelHolder h = TopazChannels.create(vertx, settings(Optional.of(true), Optional.of("n")),
                Optional.of(withTrustStore()), List.of())) {
            assertThat(h).isNotNull();
        } finally {
            vertx.close();
        }
    }
}
