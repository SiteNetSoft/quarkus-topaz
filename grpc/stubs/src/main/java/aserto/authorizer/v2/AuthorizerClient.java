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
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeRequest request) {
        return stub.decisionTree(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.IsResponse> is(aserto.authorizer.v2.AuthorizerOuterClass.IsRequest request) {
        return stub.is(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.QueryResponse> query(aserto.authorizer.v2.AuthorizerOuterClass.QueryRequest request) {
        return stub.query(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.CompileResponse> compile(aserto.authorizer.v2.AuthorizerOuterClass.CompileRequest request) {
        return stub.compile(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesRequest request) {
        return stub.listPolicies(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyResponse> getPolicy(aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyRequest request) {
        return stub.getPolicy(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.InfoResponse> info(aserto.authorizer.v2.AuthorizerOuterClass.InfoRequest request) {
        return stub.info(request);
    }
}
