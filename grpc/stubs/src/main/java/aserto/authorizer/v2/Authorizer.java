package aserto.authorizer.v2;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/authorizer/v2/authorizer.proto")
public interface Authorizer extends MutinyService {

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.DecisionTreeRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.IsResponse> is(aserto.authorizer.v2.IsRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.QueryResponse> query(aserto.authorizer.v2.QueryRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.CompileResponse> compile(aserto.authorizer.v2.CompileRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.ListPoliciesRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.GetPolicyResponse> getPolicy(aserto.authorizer.v2.GetPolicyRequest request);

    io.smallrye.mutiny.Uni<aserto.authorizer.v2.InfoResponse> info(aserto.authorizer.v2.InfoRequest request);
}
