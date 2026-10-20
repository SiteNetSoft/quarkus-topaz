package org.sitenetsoft.quarkus.topaz.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
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

    @Test
    void tlsForcedWithoutConfigUsesTlsWithJvmDefaultTrust() {
        HttpClientOptions http = TopazChannels.transportOptions(settings(Optional.of(false), Optional.empty()),
                Optional.empty());
        assertThat(http.isSsl()).isTrue();
        assertThat(http.isUseAlpn()).isTrue();
        assertThat(http.getTrustOptions()).isNull();
        assertThat(http.isTrustAll()).isFalse();
    }

    @Test
    void createWithTlsForcedButNoConfigBuildsTlsChannel() {
        Vertx vertx = Vertx.vertx();
        try (TopazChannels.ManagedChannelHolder h = TopazChannels.create(vertx,
                settings(Optional.of(false), Optional.empty()), Optional.empty(), List.of())) {
            assertThat(h).isNotNull();
        } finally {
            vertx.close();
        }
    }

    @Test
    void unsetPlainTextWithoutConfigIsPlaintext() {
        HttpClientOptions http = TopazChannels.transportOptions(settings(Optional.empty(), Optional.empty()),
                Optional.empty());
        assertThat(http.isSsl()).isFalse();
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
