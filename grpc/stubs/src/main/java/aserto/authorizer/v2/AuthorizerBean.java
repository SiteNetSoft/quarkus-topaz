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
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeRequest request) {
        try {
            return delegate.decisionTree(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.IsResponse> is(aserto.authorizer.v2.AuthorizerOuterClass.IsRequest request) {
        try {
            return delegate.is(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.QueryResponse> query(aserto.authorizer.v2.AuthorizerOuterClass.QueryRequest request) {
        try {
            return delegate.query(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.CompileResponse> compile(aserto.authorizer.v2.AuthorizerOuterClass.CompileRequest request) {
        try {
            return delegate.compile(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesRequest request) {
        try {
            return delegate.listPolicies(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyResponse> getPolicy(aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyRequest request) {
        try {
            return delegate.getPolicy(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.InfoResponse> info(aserto.authorizer.v2.AuthorizerOuterClass.InfoRequest request) {
        try {
            return delegate.info(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
