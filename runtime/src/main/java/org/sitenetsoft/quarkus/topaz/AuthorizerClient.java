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
 * Every call applies the configured deadline, if any.
 * A failed call fails the returned {@link Uni} with a {@link io.grpc.StatusRuntimeException}.
 */
public final class AuthorizerClient {

    private final MutinyAuthorizerGrpc.MutinyAuthorizerStub stub;
    private final Optional<Duration> deadline;

    /**
     * Create a client.
     *
     * @param channel the channel to the authorizer service
     * @param deadline the deadline of each call, if any
     */
    AuthorizerClient(Channel channel, Optional<Duration> deadline) {
        this.stub = MutinyAuthorizerGrpc.newMutinyStub(channel);
        this.deadline = deadline;
    }

    /**
     * Get the raw stub, with the configured deadline, if any, starting now.
     *
     * @return the raw stub
     */
    public MutinyAuthorizerGrpc.MutinyAuthorizerStub stub() {
        return deadline.map(d -> stub.withDeadlineAfter(d.toMillis(), TimeUnit.MILLISECONDS)).orElse(stub);
    }

    // Defer stub acquisition to subscription time.
    // Each subscription, including a retry or a late one, then gets a deadline that starts when the call is made.
    // A Mutiny timeout enforces the deadline too, because the gRPC deadline alone does not end a call
    // on a connection that never completes its HTTP/2 handshake.
    private <T> Uni<T> call(Function<MutinyAuthorizerGrpc.MutinyAuthorizerStub, Uni<T>> f) {
        Uni<T> call = Uni.createFrom().deferred(() -> f.apply(stub()));
        return deadline.map(d -> call.ifNoItem().after(d).failWith(() -> DirectoryClient.deadlineExceeded(d)))
                .orElse(call);
    }

    /**
     * Get the configured deadline.
     *
     * @return the deadline, if any
     */
    Optional<Duration> deadline() {
        return deadline;
    }

    /**
     * Call the authorizer {@code Is} RPC with a prepared request.
     *
     * @param r the request
     * @return the authorizer response
     */
    public Uni<IsResponse> is(IsRequest r) {
        return call(s -> s.is(r));
    }

    /**
     * Call the authorizer {@code Is} RPC for an identity, a policy path, decisions and an optional resource context.
     *
     * @param id the identity of the caller
     * @param policyPath the policy path to evaluate
     * @param decisions the decisions to evaluate
     * @param resource the resource context, or {@code null} for none
     * @return the authorizer response
     */
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
     * Evaluate one decision.
     * The call fails closed.
     * The result is {@code true} only if the response contains {@code decision} with {@code is=true}.
     * A missing decision yields {@code false}.
     * A gRPC error propagates and never maps to a boolean.
     *
     * @param id the identity of the caller
     * @param policyPath the policy path to evaluate
     * @param decision the decision to evaluate
     * @param resource the resource context, or {@code null} for none
     * @return {@code true} only if the decision is present and allowed
     */
    public Uni<Boolean> isAllowed(IdentityContext id, String policyPath, String decision, Map<String, ?> resource) {
        return is(id, policyPath, List.of(decision), Structs.of(resource))
                .map(resp -> resp.getDecisionsList().stream()
                        .anyMatch(d -> d.getDecision().equals(decision) && d.getIs()));
    }

    /**
     * Call the authorizer {@code DecisionTree} RPC.
     *
     * @param r the request
     * @return the decision tree response
     */
    public Uni<DecisionTreeResponse> decisionTree(DecisionTreeRequest r) {
        return call(s -> s.decisionTree(r));
    }

    /**
     * Call the authorizer {@code Query} RPC.
     *
     * @param r the request
     * @return the query response
     */
    public Uni<QueryResponse> query(QueryRequest r) {
        return call(s -> s.query(r));
    }

    /**
     * Call the authorizer {@code Compile} RPC.
     *
     * @param r the request
     * @return the compile response
     */
    public Uni<CompileResponse> compile(CompileRequest r) {
        return call(s -> s.compile(r));
    }

    /**
     * Call the authorizer {@code ListPolicies} RPC.
     *
     * @return the policy modules
     */
    public Uni<List<Module>> listPolicies() {
        return call(s -> s.listPolicies(ListPoliciesRequest.getDefaultInstance())).map(ListPoliciesResponse::getResultList);
    }

    /**
     * Call the authorizer {@code GetPolicy} RPC for the policy module with the given id.
     *
     * @param id the id of the policy module
     * @return the policy module
     */
    public Uni<Module> getPolicy(String id) {
        return call(s -> s.getPolicy(GetPolicyRequest.newBuilder().setId(id).build())).map(GetPolicyResponse::getResult);
    }

    /**
     * Call the authorizer {@code Info} RPC.
     *
     * @return the authorizer info response
     */
    public Uni<InfoResponse> info() {
        return call(s -> s.info(InfoRequest.getDefaultInstance()));
    }
}
