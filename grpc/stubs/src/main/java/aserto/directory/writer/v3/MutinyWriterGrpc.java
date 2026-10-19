package aserto.directory.writer.v3;

import static aserto.directory.writer.v3.WriterGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/writer/v3/writer.proto")
public final class MutinyWriterGrpc implements io.quarkus.grpc.MutinyGrpc {

    private MutinyWriterGrpc() {
    }

    public static MutinyWriterStub newMutinyStub(io.grpc.Channel channel) {
        return new MutinyWriterStub(channel);
    }

    public static class MutinyWriterStub extends io.grpc.stub.AbstractStub<MutinyWriterStub> implements io.quarkus.grpc.MutinyStub {

        private WriterGrpc.WriterStub delegateStub;

        private MutinyWriterStub(io.grpc.Channel channel) {
            super(channel);
            delegateStub = WriterGrpc.newStub(channel);
        }

        private MutinyWriterStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
            delegateStub = WriterGrpc.newStub(channel).build(channel, callOptions);
        }

        @Override
        protected MutinyWriterStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new MutinyWriterStub(channel, callOptions);
        }

        /**
         * <pre>
         *  set object instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetObjectResponse> setObject(aserto.directory.writer.v3.SetObjectRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::setObject);
        }

        /**
         * <pre>
         *  delete object instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteObjectResponse> deleteObject(aserto.directory.writer.v3.DeleteObjectRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::deleteObject);
        }

        /**
         * <pre>
         *  set relation instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetRelationResponse> setRelation(aserto.directory.writer.v3.SetRelationRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::setRelation);
        }

        /**
         * <pre>
         *  delete relation instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteRelationResponse> deleteRelation(aserto.directory.writer.v3.DeleteRelationRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::deleteRelation);
        }
    }

    public static abstract class WriterImplBase implements io.grpc.BindableService {

        private String compression;

        /**
         * Set whether the server will try to use a compressed response.
         *
         * @param compression the compression, e.g {@code gzip}
         */
        public WriterImplBase withCompression(String compression) {
            this.compression = compression;
            return this;
        }

        /**
         * <pre>
         *  set object instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetObjectResponse> setObject(aserto.directory.writer.v3.SetObjectRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  delete object instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteObjectResponse> deleteObject(aserto.directory.writer.v3.DeleteObjectRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  set relation instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetRelationResponse> setRelation(aserto.directory.writer.v3.SetRelationRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  delete relation instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteRelationResponse> deleteRelation(aserto.directory.writer.v3.DeleteRelationRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(aserto.directory.writer.v3.WriterGrpc.getSetObjectMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.SetObjectRequest, aserto.directory.writer.v3.SetObjectResponse>(this, METHODID_SET_OBJECT, compression))).addMethod(aserto.directory.writer.v3.WriterGrpc.getDeleteObjectMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.DeleteObjectRequest, aserto.directory.writer.v3.DeleteObjectResponse>(this, METHODID_DELETE_OBJECT, compression))).addMethod(aserto.directory.writer.v3.WriterGrpc.getSetRelationMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.SetRelationRequest, aserto.directory.writer.v3.SetRelationResponse>(this, METHODID_SET_RELATION, compression))).addMethod(aserto.directory.writer.v3.WriterGrpc.getDeleteRelationMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.DeleteRelationRequest, aserto.directory.writer.v3.DeleteRelationResponse>(this, METHODID_DELETE_RELATION, compression))).build();
        }
    }

    private static final int METHODID_SET_OBJECT = 0;

    private static final int METHODID_DELETE_OBJECT = 1;

    private static final int METHODID_SET_RELATION = 2;

    private static final int METHODID_DELETE_RELATION = 3;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final WriterImplBase serviceImpl;

        private final int methodId;

        private final String compression;

        MethodHandlers(WriterImplBase serviceImpl, int methodId, String compression) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
            this.compression = compression;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_SET_OBJECT:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.writer.v3.SetObjectRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.SetObjectResponse>) responseObserver, compression, serviceImpl::setObject);
                    break;
                case METHODID_DELETE_OBJECT:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.writer.v3.DeleteObjectRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.DeleteObjectResponse>) responseObserver, compression, serviceImpl::deleteObject);
                    break;
                case METHODID_SET_RELATION:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.writer.v3.SetRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.SetRelationResponse>) responseObserver, compression, serviceImpl::setRelation);
                    break;
                case METHODID_DELETE_RELATION:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.writer.v3.DeleteRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.DeleteRelationResponse>) responseObserver, compression, serviceImpl::deleteRelation);
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
