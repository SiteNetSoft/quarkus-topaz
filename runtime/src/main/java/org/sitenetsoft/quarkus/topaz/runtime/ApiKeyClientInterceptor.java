package org.sitenetsoft.quarkus.topaz.runtime;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import java.util.Optional;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Adds the Topaz API key ({@code authorization: basic <key>}) and optional tenant id to outgoing calls.
 */
public final class ApiKeyClientInterceptor implements ClientInterceptor {

    public static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> TENANT_ID = Metadata.Key.of("aserto-tenant-id", ASCII_STRING_MARSHALLER);

    private final String authorization;
    private final Optional<String> tenantId;

    public ApiKeyClientInterceptor(String apiKey, Optional<String> tenantId) {
        this.authorization = "basic " + java.util.Objects.requireNonNull(apiKey, "apiKey");
        this.tenantId = tenantId;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(AUTHORIZATION, authorization);
                tenantId.ifPresent(t -> headers.put(TENANT_ID, t));
                super.start(responseListener, headers);
            }
        };
    }
}
