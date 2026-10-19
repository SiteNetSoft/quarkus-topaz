package org.sitenetsoft.quarkus.topaz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aserto.authorizer.v2.AuthorizerGrpc;
import aserto.authorizer.v2.Decision;
import aserto.authorizer.v2.InfoRequest;
import aserto.authorizer.v2.InfoResponse;
import aserto.authorizer.v2.IsRequest;
import aserto.authorizer.v2.IsResponse;
import aserto.authorizer.v2.ListPoliciesRequest;
import aserto.authorizer.v2.ListPoliciesResponse;
import aserto.authorizer.v2.api.IdentityType;
import aserto.authorizer.v2.api.Module;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

class AuthorizerClientTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    private final AtomicReference<IsRequest> lastIs = new AtomicReference<>();
    private volatile IsResponse isResponse = IsResponse.getDefaultInstance();
    private volatile StatusRuntimeException isError;

    private Server server;
    private ManagedChannel channel;
    private TopazClient client;

    @BeforeEach
    void start() throws Exception {
        String name = "authz-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(new FakeAuthorizer()).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = TopazClient.builder().authorizerChannel(channel).directoryChannel(channel).build();
    }

    @AfterEach
    void stop() {
        client.close();
        channel.shutdownNow();
        server.shutdownNow();
    }

    private static IsResponse decisions(Decision... d) {
        return IsResponse.newBuilder().addAllDecisions(List.of(d)).build();
    }

    private static Decision decision(String name, boolean is) {
        return Decision.newBuilder().setDecision(name).setIs(is).build();
    }

    private Boolean allowed() {
        return client.authorizer()
                .isAllowed(IdentityContexts.subject("rick"), "todo.GET.todos", "allowed", Map.of("id", "1"))
                .await().atMost(WAIT);
    }

    @Test
    void isAllowedTrueWhenDecisionTrue() {
        isResponse = decisions(decision("allowed", true));

        assertThat(allowed()).isTrue();

        IsRequest r = lastIs.get();
        assertThat(r.getIdentityContext().getType()).isEqualTo(IdentityType.IDENTITY_TYPE_SUB);
        assertThat(r.getIdentityContext().getIdentity()).isEqualTo("rick");
        assertThat(r.getPolicyContext().getPath()).isEqualTo("todo.GET.todos");
        assertThat(r.getPolicyContext().getDecisionsList()).containsExactly("allowed");
        assertThat(r.getResourceContext().getFieldsMap()).containsOnlyKeys("id");
        assertThat(r.getResourceContext().getFieldsOrThrow("id").getStringValue()).isEqualTo("1");
    }

    @Test
    void isAllowedFalseWhenDecisionFalse() {
        isResponse = decisions(decision("allowed", false));
        assertThat(allowed()).isFalse();
    }

    @Test
    void isAllowedFalseWhenDecisionMissing() {
        isResponse = decisions(decision("visible", true));
        assertThat(allowed()).isFalse();
    }

    @Test
    void isPropagatesGrpcErrors() {
        isError = Status.NOT_FOUND.asRuntimeException();
        assertThatThrownBy(this::allowed)
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(e -> assertThat(((StatusRuntimeException) e).getStatus().getCode())
                        .isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void infoAndListPolicies() {
        InfoResponse info = client.authorizer().info().await().atMost(WAIT);
        assertThat(info.getVersion()).isEqualTo("0.34.1");

        List<Module> policies = client.authorizer().listPolicies().await().atMost(WAIT);
        assertThat(policies).extracting(Module::getId).containsExactly("p1", "p2");

        assertThat(client.blocking().authorizer().info().getVersion()).isEqualTo("0.34.1");
    }

    private final class FakeAuthorizer extends AuthorizerGrpc.AuthorizerImplBase {
        @Override
        public void is(IsRequest request, StreamObserver<IsResponse> responseObserver) {
            lastIs.set(request);
            if (isError != null) {
                responseObserver.onError(isError);
                return;
            }
            responseObserver.onNext(isResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void info(InfoRequest request, StreamObserver<InfoResponse> responseObserver) {
            responseObserver.onNext(InfoResponse.newBuilder().setVersion("0.34.1").build());
            responseObserver.onCompleted();
        }

        @Override
        public void listPolicies(ListPoliciesRequest request, StreamObserver<ListPoliciesResponse> responseObserver) {
            responseObserver.onNext(ListPoliciesResponse.newBuilder()
                    .addResult(Module.newBuilder().setId("p1"))
                    .addResult(Module.newBuilder().setId("p2"))
                    .build());
            responseObserver.onCompleted();
        }
    }
}
