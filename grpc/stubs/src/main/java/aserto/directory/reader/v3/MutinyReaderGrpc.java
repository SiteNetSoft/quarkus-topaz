package aserto.directory.reader.v3;

import static aserto.directory.reader.v3.ReaderGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/reader/v3/reader.proto")
public final class MutinyReaderGrpc implements io.quarkus.grpc.MutinyGrpc {

    private MutinyReaderGrpc() {
    }

    public static MutinyReaderStub newMutinyStub(io.grpc.Channel channel) {
        return new MutinyReaderStub(channel);
    }

    public static class MutinyReaderStub extends io.grpc.stub.AbstractStub<MutinyReaderStub> implements io.quarkus.grpc.MutinyStub {

        private ReaderGrpc.ReaderStub delegateStub;

        private MutinyReaderStub(io.grpc.Channel channel) {
            super(channel);
            delegateStub = ReaderGrpc.newStub(channel);
        }

        private MutinyReaderStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
            delegateStub = ReaderGrpc.newStub(channel).build(channel, callOptions);
        }

        @Override
        protected MutinyReaderStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new MutinyReaderStub(channel, callOptions);
        }

        /**
         * <pre>
         *  get object
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectResponse> getObject(aserto.directory.reader.v3.GetObjectRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::getObject);
        }

        /**
         * <pre>
         *  get multiple objects
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.GetObjectManyRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::getObjectMany);
        }

        /**
         * <pre>
         *  list objects
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectsResponse> getObjects(aserto.directory.reader.v3.GetObjectsRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::getObjects);
        }

        /**
         * <pre>
         *  get relation
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationResponse> getRelation(aserto.directory.reader.v3.GetRelationRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::getRelation);
        }

        /**
         * <pre>
         *  list relations
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationsResponse> getRelations(aserto.directory.reader.v3.GetRelationsRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::getRelations);
        }

        /**
         * <pre>
         *  check if subject has relation or permission with object
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckResponse> check(aserto.directory.reader.v3.CheckRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::check);
        }

        /**
         * <pre>
         *  checks validates a set of check requests in a single roundtrip
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ChecksResponse> checks(aserto.directory.reader.v3.ChecksRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::checks);
        }

        /**
         * <pre>
         *  check permission (deprecated, use the check method)
         *  Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @Deprecated
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.CheckPermissionRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::checkPermission);
        }

        /**
         * <pre>
         *  check relation (deprecated, use the check method)
         *  Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @Deprecated
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.CheckRelationRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::checkRelation);
        }

        /**
         * <pre>
         *  get object relationship graph
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetGraphResponse> getGraph(aserto.directory.reader.v3.GetGraphRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::getGraph);
        }
    }

    public static abstract class ReaderImplBase implements io.grpc.BindableService {

        private String compression;

        /**
         * Set whether the server will try to use a compressed response.
         *
         * @param compression the compression, e.g {@code gzip}
         */
        public ReaderImplBase withCompression(String compression) {
            this.compression = compression;
            return this;
        }

        /**
         * <pre>
         *  get object
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectResponse> getObject(aserto.directory.reader.v3.GetObjectRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  get multiple objects
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.GetObjectManyRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  list objects
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectsResponse> getObjects(aserto.directory.reader.v3.GetObjectsRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  get relation
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationResponse> getRelation(aserto.directory.reader.v3.GetRelationRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  list relations
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationsResponse> getRelations(aserto.directory.reader.v3.GetRelationsRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  check if subject has relation or permission with object
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckResponse> check(aserto.directory.reader.v3.CheckRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  checks validates a set of check requests in a single roundtrip
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ChecksResponse> checks(aserto.directory.reader.v3.ChecksRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  check permission (deprecated, use the check method)
         *  Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @Deprecated
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.CheckPermissionRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  check relation (deprecated, use the check method)
         *  Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @Deprecated
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.CheckRelationRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  get object relationship graph
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetGraphResponse> getGraph(aserto.directory.reader.v3.GetGraphRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(aserto.directory.reader.v3.ReaderGrpc.getGetObjectMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.GetObjectRequest, aserto.directory.reader.v3.GetObjectResponse>(this, METHODID_GET_OBJECT, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getGetObjectManyMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.GetObjectManyRequest, aserto.directory.reader.v3.GetObjectManyResponse>(this, METHODID_GET_OBJECT_MANY, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getGetObjectsMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.GetObjectsRequest, aserto.directory.reader.v3.GetObjectsResponse>(this, METHODID_GET_OBJECTS, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getGetRelationMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.GetRelationRequest, aserto.directory.reader.v3.GetRelationResponse>(this, METHODID_GET_RELATION, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getGetRelationsMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.GetRelationsRequest, aserto.directory.reader.v3.GetRelationsResponse>(this, METHODID_GET_RELATIONS, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getCheckMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.CheckRequest, aserto.directory.reader.v3.CheckResponse>(this, METHODID_CHECK, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getChecksMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ChecksRequest, aserto.directory.reader.v3.ChecksResponse>(this, METHODID_CHECKS, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getCheckPermissionMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.CheckPermissionRequest, aserto.directory.reader.v3.CheckPermissionResponse>(this, METHODID_CHECK_PERMISSION, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getCheckRelationMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.CheckRelationRequest, aserto.directory.reader.v3.CheckRelationResponse>(this, METHODID_CHECK_RELATION, compression))).addMethod(aserto.directory.reader.v3.ReaderGrpc.getGetGraphMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.GetGraphRequest, aserto.directory.reader.v3.GetGraphResponse>(this, METHODID_GET_GRAPH, compression))).build();
        }
    }

    private static final int METHODID_GET_OBJECT = 0;

    private static final int METHODID_GET_OBJECT_MANY = 1;

    private static final int METHODID_GET_OBJECTS = 2;

    private static final int METHODID_GET_RELATION = 3;

    private static final int METHODID_GET_RELATIONS = 4;

    private static final int METHODID_CHECK = 5;

    private static final int METHODID_CHECKS = 6;

    private static final int METHODID_CHECK_PERMISSION = 7;

    private static final int METHODID_CHECK_RELATION = 8;

    private static final int METHODID_GET_GRAPH = 9;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final ReaderImplBase serviceImpl;

        private final int methodId;

        private final String compression;

        MethodHandlers(ReaderImplBase serviceImpl, int methodId, String compression) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
            this.compression = compression;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_GET_OBJECT:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.GetObjectRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.GetObjectResponse>) responseObserver, compression, serviceImpl::getObject);
                    break;
                case METHODID_GET_OBJECT_MANY:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.GetObjectManyRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.GetObjectManyResponse>) responseObserver, compression, serviceImpl::getObjectMany);
                    break;
                case METHODID_GET_OBJECTS:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.GetObjectsRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.GetObjectsResponse>) responseObserver, compression, serviceImpl::getObjects);
                    break;
                case METHODID_GET_RELATION:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.GetRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.GetRelationResponse>) responseObserver, compression, serviceImpl::getRelation);
                    break;
                case METHODID_GET_RELATIONS:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.GetRelationsRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.GetRelationsResponse>) responseObserver, compression, serviceImpl::getRelations);
                    break;
                case METHODID_CHECK:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.CheckRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.CheckResponse>) responseObserver, compression, serviceImpl::check);
                    break;
                case METHODID_CHECKS:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.ChecksRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ChecksResponse>) responseObserver, compression, serviceImpl::checks);
                    break;
                case METHODID_CHECK_PERMISSION:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.CheckPermissionRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.CheckPermissionResponse>) responseObserver, compression, serviceImpl::checkPermission);
                    break;
                case METHODID_CHECK_RELATION:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.CheckRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.CheckRelationResponse>) responseObserver, compression, serviceImpl::checkRelation);
                    break;
                case METHODID_GET_GRAPH:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.reader.v3.GetGraphRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.GetGraphResponse>) responseObserver, compression, serviceImpl::getGraph);
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
