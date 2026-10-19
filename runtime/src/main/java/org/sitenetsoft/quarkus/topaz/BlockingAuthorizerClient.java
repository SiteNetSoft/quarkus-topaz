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
 * Blocking view of {@link AuthorizerClient}. Each call waits at most the configured deadline (30 s if none).
 * Must not be called on an event-loop thread.
 */
public final class BlockingAuthorizerClient {

    private final AuthorizerClient delegate;
    private final Duration timeout;

    BlockingAuthorizerClient(AuthorizerClient delegate) {
        this.delegate = delegate;
        this.timeout = delegate.deadline().orElse(BlockingTopazClient.DEFAULT_TIMEOUT);
    }

    private <T> T await(Uni<T> uni) {
        return uni.await().atMost(timeout);
    }

    public IsResponse is(IsRequest r) {
        return await(delegate.is(r));
    }

    public IsResponse is(IdentityContext id, String policyPath, List<String> decisions, Struct resource) {
        return await(delegate.is(id, policyPath, decisions, resource));
    }

    public boolean isAllowed(IdentityContext id, String policyPath, String decision, Map<String, ?> resource) {
        return await(delegate.isAllowed(id, policyPath, decision, resource));
    }

    public DecisionTreeResponse decisionTree(DecisionTreeRequest r) {
        return await(delegate.decisionTree(r));
    }

    public QueryResponse query(QueryRequest r) {
        return await(delegate.query(r));
    }

    public CompileResponse compile(CompileRequest r) {
        return await(delegate.compile(r));
    }

    public List<Module> listPolicies() {
        return await(delegate.listPolicies());
    }

    public Module getPolicy(String id) {
        return await(delegate.getPolicy(id));
    }

    public InfoResponse info() {
        return await(delegate.info());
    }
}
