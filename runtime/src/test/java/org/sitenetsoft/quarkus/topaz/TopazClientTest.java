package org.sitenetsoft.quarkus.topaz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import aserto.authorizer.v2.AuthorizerGrpc;
import aserto.authorizer.v2.InfoRequest;
import aserto.authorizer.v2.InfoResponse;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;

class TopazClientTest {

    @Test
    void deadlineAndApiKeyAppliedToGivenChannels() throws Exception {
        AtomicReference<Deadline> deadline = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        ServerInterceptor capture = new ServerInterceptor() {
            @Override
            public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata headers,
                    ServerCallHandler<Q, R> next) {
                auth.set(headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)));
                deadline.set(Context.current().getDeadline());
                return next.startCall(call, headers);
            }
        };
        String name = "client-" + UUID.randomUUID();
        Server server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(ServerInterceptors.intercept(new AuthorizerGrpc.AuthorizerImplBase() {
                    @Override
                    public void info(InfoRequest request, StreamObserver<InfoResponse> responseObserver) {
                        responseObserver.onNext(InfoResponse.getDefaultInstance());
                        responseObserver.onCompleted();
                    }
                }, capture)).build().start();
        ManagedChannel ch = InProcessChannelBuilder.forName(name).directExecutor().build();
        try (TopazClient client = TopazClient.builder().authorizerChannel(ch).directoryChannel(ch)
                .apiKey("k").deadline(Duration.ofSeconds(7)).build()) {
            client.blocking().authorizer().info();
        } finally {
            ch.shutdownNow();
            server.shutdownNow();
        }
        assertThat(auth.get()).isEqualTo("basic k");
        assertThat(deadline.get()).isNotNull();
        assertThat(deadline.get().timeRemaining(TimeUnit.SECONDS)).isBetween(1L, 7L);
    }

    @Test
    void buildCreatesAndClosesChannels() {
        TopazClient.builder().plainText(true).authorizer("localhost", 1).directory("localhost", 2).build().close();
    }

    @Test
    void buildFailsClosedWithoutTlsAndDoesNotCloseCallerVertx() {
        Vertx vertx = Vertx.vertx();
        try {
            assertThatThrownBy(() -> TopazClient.builder().vertx(vertx).plainText(false).build())
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("localhost:8282");
            CountDownLatch ran = new CountDownLatch(1);
            vertx.runOnContext(x -> ran.countDown());
            assertThat(ran.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            vertx.close();
        }
    }
}
