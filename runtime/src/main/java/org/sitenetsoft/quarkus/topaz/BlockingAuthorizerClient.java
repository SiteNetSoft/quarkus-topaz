package org.sitenetsoft.quarkus.topaz;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Struct;

import aserto.authorizer.v2.CompileRequest;
import aserto.authorizer.v2.CompileResponse;
import aserto.authorizer.v2.DecisionTreeRequest;
import aserto.authorizer.v2.DecisionTreeResponse;
import aserto.authorizer.v2.InfoResponse;
import aserto.authorizer.v2.IsRequest;
import aserto.authorizer.v2.IsResponse;
import aserto.authorizer.v2.QueryRequest;
import aserto.authorizer.v2.QueryResponse;
import aserto.authorizer.v2.api.IdentityContext;
import aserto.authorizer.v2.api.Module;
import io.smallrye.mutiny.Uni;

/**
 * Blocking view of {@link AuthorizerClient}.
 * Each call waits at most the configured deadline plus one second, or 30 seconds if no deadline is set.
 * Failures, including an exceeded wait, surface as {@link io.grpc.StatusRuntimeException}.
 * Do not call it on an event-loop thread.
 */
public final class BlockingAuthorizerClient {

    private final AuthorizerClient delegate;
    private final Duration timeout;

    /**
     * Create a blocking view of an authorizer client.
     *
     * @param delegate the reactive client to wrap
     */
    BlockingAuthorizerClient(AuthorizerClient delegate) {
        this.delegate = delegate;
        this.timeout = BlockingTopazClient.waitFor(delegate.deadline());
    }

    private <T> T await(Uni<T> uni) {
        return BlockingTopazClient.await(uni, timeout);
    }

    /**
     * Block until {@link AuthorizerClient#is(IsRequest)} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param r the request
     * @return the authorizer response
     */
    public IsResponse is(IsRequest r) {
        return await(delegate.is(r));
    }

    /**
     * Block until {@link AuthorizerClient#is(IdentityContext, String, List, Struct)} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param id the identity of the caller
     * @param policyPath the policy path to evaluate
     * @param decisions the decisions to evaluate
     * @param resource the resource context, or {@code null} for none
     * @return the authorizer response
     */
    public IsResponse is(IdentityContext id, String policyPath, List<String> decisions, Struct resource) {
        return await(delegate.is(id, policyPath, decisions, resource));
    }

    /**
     * Block until {@link AuthorizerClient#isAllowed} completes.
     * Fail closed: a missing decision yields {@code false}.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param id the identity of the caller
     * @param policyPath the policy path to evaluate
     * @param decision the decision to evaluate
     * @param resource the resource context, or {@code null} for none
     * @return {@code true} only if the decision is present and allowed
     */
    public boolean isAllowed(IdentityContext id, String policyPath, String decision, Map<String, ?> resource) {
        return await(delegate.isAllowed(id, policyPath, decision, resource));
    }

    /**
     * Block until {@link AuthorizerClient#decisionTree} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param r the request
     * @return the decision tree response
     */
    public DecisionTreeResponse decisionTree(DecisionTreeRequest r) {
        return await(delegate.decisionTree(r));
    }

    /**
     * Block until {@link AuthorizerClient#query} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param r the request
     * @return the query response
     */
    public QueryResponse query(QueryRequest r) {
        return await(delegate.query(r));
    }

    /**
     * Block until {@link AuthorizerClient#compile} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param r the request
     * @return the compile response
     */
    public CompileResponse compile(CompileRequest r) {
        return await(delegate.compile(r));
    }

    /**
     * Block until {@link AuthorizerClient#listPolicies} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @return the policy modules
     */
    public List<Module> listPolicies() {
        return await(delegate.listPolicies());
    }

    /**
     * Block until {@link AuthorizerClient#getPolicy} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param id the id of the policy module
     * @return the policy module
     */
    public Module getPolicy(String id) {
        return await(delegate.getPolicy(id));
    }

    /**
     * Block until {@link AuthorizerClient#info} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @return the authorizer info response
     */
    public InfoResponse info() {
        return await(delegate.info());
    }
}
