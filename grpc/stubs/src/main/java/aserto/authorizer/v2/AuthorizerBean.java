package aserto.authorizer.v2;

import io.grpc.BindableService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.MutinyBean;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/authorizer/v2/authorizer.proto")
public class AuthorizerBean extends MutinyAuthorizerGrpc.AuthorizerImplBase implements BindableService, MutinyBean {

    private final Authorizer delegate;

    AuthorizerBean(@GrpcService Authorizer delegate) {
        this.delegate = delegate;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.DecisionTreeRequest request) {
        try {
            return delegate.decisionTree(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.IsResponse> is(aserto.authorizer.v2.IsRequest request) {
        try {
            return delegate.is(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.QueryResponse> query(aserto.authorizer.v2.QueryRequest request) {
        try {
            return delegate.query(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.CompileResponse> compile(aserto.authorizer.v2.CompileRequest request) {
        try {
            return delegate.compile(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.ListPoliciesRequest request) {
        try {
            return delegate.listPolicies(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.GetPolicyResponse> getPolicy(aserto.authorizer.v2.GetPolicyRequest request) {
        try {
            return delegate.getPolicy(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.InfoResponse> info(aserto.authorizer.v2.InfoRequest request) {
        try {
            return delegate.info(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
