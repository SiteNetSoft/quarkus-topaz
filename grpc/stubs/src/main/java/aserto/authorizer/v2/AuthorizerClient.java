package aserto.authorizer.v2;

import java.util.function.BiFunction;
import io.quarkus.grpc.MutinyClient;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/authorizer/v2/authorizer.proto")
public class AuthorizerClient implements Authorizer, MutinyClient<MutinyAuthorizerGrpc.MutinyAuthorizerStub> {

    private final MutinyAuthorizerGrpc.MutinyAuthorizerStub stub;

    public AuthorizerClient(String name, io.grpc.Channel channel, BiFunction<String, MutinyAuthorizerGrpc.MutinyAuthorizerStub, MutinyAuthorizerGrpc.MutinyAuthorizerStub> stubConfigurator) {
        this.stub = stubConfigurator.apply(name, MutinyAuthorizerGrpc.newMutinyStub(channel));
    }

    private AuthorizerClient(MutinyAuthorizerGrpc.MutinyAuthorizerStub stub) {
        this.stub = stub;
    }

    public AuthorizerClient newInstanceWithStub(MutinyAuthorizerGrpc.MutinyAuthorizerStub stub) {
        return new AuthorizerClient(stub);
    }

    @Override
    public MutinyAuthorizerGrpc.MutinyAuthorizerStub getStub() {
        return stub;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.DecisionTreeRequest request) {
        return stub.decisionTree(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.IsResponse> is(aserto.authorizer.v2.IsRequest request) {
        return stub.is(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.QueryResponse> query(aserto.authorizer.v2.QueryRequest request) {
        return stub.query(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.CompileResponse> compile(aserto.authorizer.v2.CompileRequest request) {
        return stub.compile(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.ListPoliciesRequest request) {
        return stub.listPolicies(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.GetPolicyResponse> getPolicy(aserto.authorizer.v2.GetPolicyRequest request) {
        return stub.getPolicy(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.InfoResponse> info(aserto.authorizer.v2.InfoRequest request) {
        return stub.info(request);
    }
}
