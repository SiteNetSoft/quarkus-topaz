package aserto.authorizer.v2;

import static aserto.authorizer.v2.AuthorizerGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/authorizer/v2/authorizer.proto")
public final class MutinyAuthorizerGrpc implements io.quarkus.grpc.MutinyGrpc {

    private MutinyAuthorizerGrpc() {
    }

    public static MutinyAuthorizerStub newMutinyStub(io.grpc.Channel channel) {
        return new MutinyAuthorizerStub(channel);
    }

    public static class MutinyAuthorizerStub extends io.grpc.stub.AbstractStub<MutinyAuthorizerStub> implements io.quarkus.grpc.MutinyStub {

        private AuthorizerGrpc.AuthorizerStub delegateStub;

        private MutinyAuthorizerStub(io.grpc.Channel channel) {
            super(channel);
            delegateStub = AuthorizerGrpc.newStub(channel);
        }

        private MutinyAuthorizerStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
            delegateStub = AuthorizerGrpc.newStub(channel).build(channel, callOptions);
        }

        @Override
        protected MutinyAuthorizerStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new MutinyAuthorizerStub(channel, callOptions);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::decisionTree);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.IsResponse> is(aserto.authorizer.v2.AuthorizerOuterClass.IsRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::is);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.QueryResponse> query(aserto.authorizer.v2.AuthorizerOuterClass.QueryRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::query);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.CompileResponse> compile(aserto.authorizer.v2.AuthorizerOuterClass.CompileRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::compile);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::listPolicies);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyResponse> getPolicy(aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::getPolicy);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.InfoResponse> info(aserto.authorizer.v2.AuthorizerOuterClass.InfoRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::info);
        }
    }

    public static abstract class AuthorizerImplBase implements io.grpc.BindableService {

        private String compression;

        /**
         * Set whether the server will try to use a compressed response.
         *
         * @param compression the compression, e.g {@code gzip}
         */
        public AuthorizerImplBase withCompression(String compression) {
            this.compression = compression;
            return this;
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeResponse> decisionTree(aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.IsResponse> is(aserto.authorizer.v2.AuthorizerOuterClass.IsRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.QueryResponse> query(aserto.authorizer.v2.AuthorizerOuterClass.QueryRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.CompileResponse> compile(aserto.authorizer.v2.AuthorizerOuterClass.CompileRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesResponse> listPolicies(aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyResponse> getPolicy(aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        public io.smallrye.mutiny.Uni<aserto.authorizer.v2.AuthorizerOuterClass.InfoResponse> info(aserto.authorizer.v2.AuthorizerOuterClass.InfoRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(aserto.authorizer.v2.AuthorizerGrpc.getDecisionTreeMethod(), asyncUnaryCall(new MethodHandlers<aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeRequest, aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeResponse>(this, METHODID_DECISION_TREE, compression))).addMethod(aserto.authorizer.v2.AuthorizerGrpc.getIsMethod(), asyncUnaryCall(new MethodHandlers<aserto.authorizer.v2.AuthorizerOuterClass.IsRequest, aserto.authorizer.v2.AuthorizerOuterClass.IsResponse>(this, METHODID_IS, compression))).addMethod(aserto.authorizer.v2.AuthorizerGrpc.getQueryMethod(), asyncUnaryCall(new MethodHandlers<aserto.authorizer.v2.AuthorizerOuterClass.QueryRequest, aserto.authorizer.v2.AuthorizerOuterClass.QueryResponse>(this, METHODID_QUERY, compression))).addMethod(aserto.authorizer.v2.AuthorizerGrpc.getCompileMethod(), asyncUnaryCall(new MethodHandlers<aserto.authorizer.v2.AuthorizerOuterClass.CompileRequest, aserto.authorizer.v2.AuthorizerOuterClass.CompileResponse>(this, METHODID_COMPILE, compression))).addMethod(aserto.authorizer.v2.AuthorizerGrpc.getListPoliciesMethod(), asyncUnaryCall(new MethodHandlers<aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesRequest, aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesResponse>(this, METHODID_LIST_POLICIES, compression))).addMethod(aserto.authorizer.v2.AuthorizerGrpc.getGetPolicyMethod(), asyncUnaryCall(new MethodHandlers<aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyRequest, aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyResponse>(this, METHODID_GET_POLICY, compression))).addMethod(aserto.authorizer.v2.AuthorizerGrpc.getInfoMethod(), asyncUnaryCall(new MethodHandlers<aserto.authorizer.v2.AuthorizerOuterClass.InfoRequest, aserto.authorizer.v2.AuthorizerOuterClass.InfoResponse>(this, METHODID_INFO, compression))).build();
        }
    }

    private static final int METHODID_DECISION_TREE = 0;

    private static final int METHODID_IS = 1;

    private static final int METHODID_QUERY = 2;

    private static final int METHODID_COMPILE = 3;

    private static final int METHODID_LIST_POLICIES = 4;

    private static final int METHODID_GET_POLICY = 5;

    private static final int METHODID_INFO = 6;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final AuthorizerImplBase serviceImpl;

        private final int methodId;

        private final String compression;

        MethodHandlers(AuthorizerImplBase serviceImpl, int methodId, String compression) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
            this.compression = compression;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_DECISION_TREE:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeRequest) request, (io.grpc.stub.StreamObserver<aserto.authorizer.v2.AuthorizerOuterClass.DecisionTreeResponse>) responseObserver, compression, serviceImpl::decisionTree);
                    break;
                case METHODID_IS:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.authorizer.v2.AuthorizerOuterClass.IsRequest) request, (io.grpc.stub.StreamObserver<aserto.authorizer.v2.AuthorizerOuterClass.IsResponse>) responseObserver, compression, serviceImpl::is);
                    break;
                case METHODID_QUERY:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.authorizer.v2.AuthorizerOuterClass.QueryRequest) request, (io.grpc.stub.StreamObserver<aserto.authorizer.v2.AuthorizerOuterClass.QueryResponse>) responseObserver, compression, serviceImpl::query);
                    break;
                case METHODID_COMPILE:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.authorizer.v2.AuthorizerOuterClass.CompileRequest) request, (io.grpc.stub.StreamObserver<aserto.authorizer.v2.AuthorizerOuterClass.CompileResponse>) responseObserver, compression, serviceImpl::compile);
                    break;
                case METHODID_LIST_POLICIES:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesRequest) request, (io.grpc.stub.StreamObserver<aserto.authorizer.v2.AuthorizerOuterClass.ListPoliciesResponse>) responseObserver, compression, serviceImpl::listPolicies);
                    break;
                case METHODID_GET_POLICY:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyRequest) request, (io.grpc.stub.StreamObserver<aserto.authorizer.v2.AuthorizerOuterClass.GetPolicyResponse>) responseObserver, compression, serviceImpl::getPolicy);
                    break;
                case METHODID_INFO:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.authorizer.v2.AuthorizerOuterClass.InfoRequest) request, (io.grpc.stub.StreamObserver<aserto.authorizer.v2.AuthorizerOuterClass.InfoResponse>) responseObserver, compression, serviceImpl::info);
                    break;
                default:
                    throw new java.lang.AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                default:
                    throw new java.lang.AssertionError();
            }
        }
    }
}
