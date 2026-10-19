package org.sitenetsoft.quarkus.topaz.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import aserto.authorizer.v2.AuthorizerGrpc;
import aserto.authorizer.v2.InfoRequest;
import aserto.authorizer.v2.InfoResponse;
import io.grpc.ClientInterceptors;
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

class ApiKeyClientInterceptorTest {

    private Metadata run(ApiKeyClientInterceptor interceptor, String name) throws Exception {
        AtomicReference<Metadata> captured = new AtomicReference<>();
        ServerInterceptor capture = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                captured.set(headers);
                return next.startCall(call, headers);
            }
        };
        Server server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(ServerInterceptors.intercept(new AuthorizerGrpc.AuthorizerImplBase() {
                    @Override
                    public void info(InfoRequest request, StreamObserver<InfoResponse> out) {
                        out.onNext(InfoResponse.getDefaultInstance());
                        out.onCompleted();
                    }
                }, capture)).build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        try {
            AuthorizerGrpc.newBlockingStub(ClientInterceptors.intercept(channel, interceptor))
                    .info(InfoRequest.getDefaultInstance());
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
        return captured.get();
    }

    @Test
    void sendsBasicApiKeyAndTenant() throws Exception {
        Metadata md = run(new ApiKeyClientInterceptor("k1", Optional.of("ten")), "t1");
        assertThat(md.get(ApiKeyClientInterceptor.AUTHORIZATION)).isEqualTo("basic k1");
        assertThat(md.get(ApiKeyClientInterceptor.TENANT_ID)).isEqualTo("ten");
    }

    @Test
    void omitsTenantWhenAbsent() throws Exception {
        Metadata md = run(new ApiKeyClientInterceptor("k2", Optional.empty()), "t2");
        assertThat(md.get(ApiKeyClientInterceptor.AUTHORIZATION)).isEqualTo("basic k2");
        assertThat(md.get(ApiKeyClientInterceptor.TENANT_ID)).isNull();
    }
}
