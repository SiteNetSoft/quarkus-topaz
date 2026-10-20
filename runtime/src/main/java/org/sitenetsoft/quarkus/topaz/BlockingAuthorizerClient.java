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
 * Blocking view of {@link AuthorizerClient}. Each call waits at most the configured deadline plus one second (30 s if none);
 * failures, including an exceeded wait, surface as {@link io.grpc.StatusRuntimeException}.
 * Must not be called on an event-loop thread.
 */
public final class BlockingAuthorizerClient {

    private final AuthorizerClient delegate;
    private final Duration timeout;

    BlockingAuthorizerClient(AuthorizerClient delegate) {
        this.delegate = delegate;
        this.timeout = BlockingTopazClient.waitFor(delegate.deadline());
    }

    private <T> T await(Uni<T> uni) {
        return BlockingTopazClient.await(uni, timeout);
    }

    /**
     * Blocking form of {@link AuthorizerClient#is(IsRequest)}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public IsResponse is(IsRequest r) {
        return await(delegate.is(r));
    }

    /**
     * Blocking form of {@link AuthorizerClient#is(IdentityContext, String, List, Struct)}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public IsResponse is(IdentityContext id, String policyPath, List<String> decisions, Struct resource) {
        return await(delegate.is(id, policyPath, decisions, resource));
    }

    /**
     * Blocking form of {@link AuthorizerClient#isAllowed}; fails closed, returning {@code true} only if the decision is present and allowed. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public boolean isAllowed(IdentityContext id, String policyPath, String decision, Map<String, ?> resource) {
        return await(delegate.isAllowed(id, policyPath, decision, resource));
    }

    /**
     * Blocking form of {@link AuthorizerClient#decisionTree}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public DecisionTreeResponse decisionTree(DecisionTreeRequest r) {
        return await(delegate.decisionTree(r));
    }

    /**
     * Blocking form of {@link AuthorizerClient#query}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public QueryResponse query(QueryRequest r) {
        return await(delegate.query(r));
    }

    /**
     * Blocking form of {@link AuthorizerClient#compile}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public CompileResponse compile(CompileRequest r) {
        return await(delegate.compile(r));
    }

    /**
     * Blocking form of {@link AuthorizerClient#listPolicies}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public List<Module> listPolicies() {
        return await(delegate.listPolicies());
    }

    /**
     * Blocking form of {@link AuthorizerClient#getPolicy}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public Module getPolicy(String id) {
        return await(delegate.getPolicy(id));
    }

    /**
     * Blocking form of {@link AuthorizerClient#info}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public InfoResponse info() {
        return await(delegate.info());
    }
}
