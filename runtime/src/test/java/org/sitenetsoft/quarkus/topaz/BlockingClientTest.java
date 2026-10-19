package org.sitenetsoft.quarkus.topaz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import aserto.authorizer.v2.AuthorizerGrpc;
import aserto.authorizer.v2.InfoRequest;
import aserto.authorizer.v2.InfoResponse;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Uni;

class BlockingClientTest {

    @Test
    void neverAnsweringServerFailsWithDeadlineExceededStatus() throws Exception {
        String name = "silent-" + UUID.randomUUID();
        Server server = InProcessServerBuilder.forName(name)
                .addService(new AuthorizerGrpc.AuthorizerImplBase() {
                    @Override
                    public void info(InfoRequest request, StreamObserver<InfoResponse> responseObserver) {
                        // never answers
                    }
                }).build().start();
        ManagedChannel ch = InProcessChannelBuilder.forName(name).build();
        try (TopazClient client = TopazClient.builder().authorizerChannel(ch).directoryChannel(ch)
                .deadline(Duration.ofMillis(200)).build()) {
            Throwable t = catchThrowable(() -> client.blocking().authorizer().info());
            assertThat(t).isInstanceOf(StatusRuntimeException.class);
            assertThat(((StatusRuntimeException) t).getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
        } finally {
            ch.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    void exceededBlockingWaitMapsToDeadlineExceededStatus() {
        Throwable t = catchThrowable(() -> BlockingTopazClient.await(Uni.createFrom().nothing(), Duration.ofMillis(50)));
        assertThat(t).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException e = (StatusRuntimeException) t;
        assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
        assertThat(e.getStatus().getDescription()).isEqualTo("blocking wait exceeded");
    }

    @Test
    void waitIsDeadlinePlusOneSecondOrThirtySeconds() {
        assertThat(BlockingTopazClient.waitFor(Optional.of(Duration.ofSeconds(2)))).isEqualTo(Duration.ofSeconds(3));
        assertThat(BlockingTopazClient.waitFor(Optional.empty())).isEqualTo(Duration.ofSeconds(30));
    }
}
