package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import java.time.Duration;
import java.util.Optional;

import org.sitenetsoft.quarkus.topaz.TopazClient;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.Vertx;

/**
 * Ready once {@code Authorizer.Info} answers and {@code Model.GetManifest} answers or reports {@code NOT_FOUND}
 * (no manifest yet), i.e. both gRPC listeners serve requests, not merely accept connections.
 */
final class TopazReadyWaitStrategy extends AbstractWaitStrategy {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(2);
    private static final long PAUSE_MS = 250;

    private final Optional<String> apiKey;

    TopazReadyWaitStrategy(Optional<String> apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void waitUntilReady() {
        long deadline = System.nanoTime() + startupTimeout.toNanos();
        Vertx vertx = Vertx.vertx();
        Throwable last = null;
        try (TopazClient client = client(vertx)) {
            while (System.nanoTime() < deadline) {
                try {
                    client.authorizer().info().await().atMost(CALL_TIMEOUT);
                    try {
                        client.directory().getManifest().await().atMost(CALL_TIMEOUT);
                    } catch (StatusRuntimeException e) {
                        if (e.getStatus().getCode() != Status.Code.NOT_FOUND) {
                            throw e;
                        }
                    }
                    return;
                } catch (RuntimeException e) {
                    last = e;
                }
                try {
                    Thread.sleep(PAUSE_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ContainerLaunchException("Interrupted while waiting for Topaz", e);
                }
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
        throw new ContainerLaunchException("Topaz did not become ready within " + startupTimeout
                + (last == null ? "" : ": " + last), last);
    }

    private TopazClient client(Vertx vertx) {
        String host = waitStrategyTarget.getHost();
        TopazClient.Builder b = TopazClient.builder()
                .authorizer(host, waitStrategyTarget.getMappedPort(TopazConfigFile.AUTHORIZER_PORT))
                .directory(host, waitStrategyTarget.getMappedPort(TopazConfigFile.DIRECTORY_PORT))
                .plainText(true)
                .vertx(vertx);
        apiKey.ifPresent(b::apiKey);
        return b.build();
    }
}
