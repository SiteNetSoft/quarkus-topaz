package aserto.authorizer.v2;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/authorizer/v2/authorizer.proto")
public interface Authorizer extends MutinyService {

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.IsResponse> is(aserto.authorizer.v2.AuthorizerOuterClass.IsRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.QueryResponse> query(aserto.authorizer.v2.AuthorizerOuterClass.QueryRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.CompileResponse> compile(aserto.authorizer.v2.AuthorizerOuterClass.CompileRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyResponse> getPolicy(aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.InfoResponse> info(aserto.authorizer.v2.AuthorizerOuterClass.InfoRequest request);
}
