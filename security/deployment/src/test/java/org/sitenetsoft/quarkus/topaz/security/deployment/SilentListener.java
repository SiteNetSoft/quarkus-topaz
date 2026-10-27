package org.sitenetsoft.quarkus.topaz.security.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;

final class SilentListener {

    private SilentListener() {
    }

    // Accepted connections queue in the backlog and are never read, so the HTTP/2 handshake never completes.
    static ServerSocket open() {
        try {
            return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
