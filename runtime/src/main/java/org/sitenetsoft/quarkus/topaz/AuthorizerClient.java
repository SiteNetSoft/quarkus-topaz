package org.sitenetsoft.quarkus.topaz;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.protobuf.Struct;

import aserto.authorizer.v2.CompileRequest;
import aserto.authorizer.v2.CompileResponse;
import aserto.authorizer.v2.DecisionTreeRequest;
import aserto.authorizer.v2.DecisionTreeResponse;
import aserto.authorizer.v2.GetPolicyRequest;
import aserto.authorizer.v2.GetPolicyResponse;
import aserto.authorizer.v2.InfoRequest;
import aserto.authorizer.v2.InfoResponse;
import aserto.authorizer.v2.IsRequest;
import aserto.authorizer.v2.IsResponse;
import aserto.authorizer.v2.ListPoliciesRequest;
import aserto.authorizer.v2.ListPoliciesResponse;
import aserto.authorizer.v2.MutinyAuthorizerGrpc;
import aserto.authorizer.v2.QueryRequest;
import aserto.authorizer.v2.QueryResponse;
import aserto.authorizer.v2.api.IdentityContext;
import aserto.authorizer.v2.api.Module;
import aserto.authorizer.v2.api.PolicyContext;
import io.grpc.Channel;
import io.smallrye.mutiny.Uni;

/**
 * Reactive client of the Topaz authorizer service.
 */
public final class AuthorizerClient {

    private final MutinyAuthorizerGrpc.MutinyAuthorizerStub stub;
    private final Optional<Duration> deadline;

    AuthorizerClient(Channel channel, Optional<Duration> deadline) {
        this.stub = MutinyAuthorizerGrpc.newMutinyStub(channel);
        this.deadline = deadline;
    }

    /** The raw stub, with the configured deadline (if any) starting now. */
    public MutinyAuthorizerGrpc.MutinyAuthorizerStub stub() {
        return deadline.map(d -> stub.withDeadlineAfter(d.toMillis(), TimeUnit.MILLISECONDS)).orElse(stub);
    }

    /**
     * Defers stub acquisition to subscription time, so each subscription (including retries and late
     * subscriptions) gets a deadline that starts when the call is actually made.
     */
    private <T> Uni<T> call(Function<MutinyAuthorizerGrpc.MutinyAuthorizerStub, Uni<T>> f) {
        return Uni.createFrom().deferred(() -> f.apply(stub()));
    }

    Optional<Duration> deadline() {
        return deadline;
    }

    public Uni<IsResponse> is(IsRequest r) {
        return call(s -> s.is(r));
    }

    public Uni<IsResponse> is(IdentityContext id, String policyPath, List<String> decisions, Struct resource) {
        IsRequest.Builder b = IsRequest.newBuilder()
                .setIdentityContext(id)
                .setPolicyContext(PolicyContext.newBuilder().setPath(policyPath).addAllDecisions(decisions));
        if (resource != null) {
            b.setResourceContext(resource);
        }
        return is(b.build());
    }

    /**
     * Evaluates one decision. Fails closed: {@code true} only if the response contains {@code decision} with
     * {@code is=true}; a missing decision yields {@code false}. gRPC errors are propagated, never mapped to a
     * boolean.
     */
    public Uni<Boolean> isAllowed(IdentityContext id, String policyPath, String decision, Map<String, ?> resource) {
        return is(id, policyPath, List.of(decision), Structs.of(resource))
                .map(resp -> resp.getDecisionsList().stream()
                        .anyMatch(d -> d.getDecision().equals(decision) && d.getIs()));
    }

    public Uni<DecisionTreeResponse> decisionTree(DecisionTreeRequest r) {
        return call(s -> s.decisionTree(r));
    }

    public Uni<QueryResponse> query(QueryRequest r) {
        return call(s -> s.query(r));
    }

    public Uni<CompileResponse> compile(CompileRequest r) {
        return call(s -> s.compile(r));
    }

    public Uni<List<Module>> listPolicies() {
        return call(s -> s.listPolicies(ListPoliciesRequest.getDefaultInstance())).map(ListPoliciesResponse::getResultList);
    }

    public Uni<Module> getPolicy(String id) {
        return call(s -> s.getPolicy(GetPolicyRequest.newBuilder().setId(id).build())).map(GetPolicyResponse::getResult);
    }

    public Uni<InfoResponse> info() {
        return call(s -> s.info(InfoRequest.getDefaultInstance()));
    }
}
