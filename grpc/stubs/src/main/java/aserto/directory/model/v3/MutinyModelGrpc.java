package aserto.directory.model.v3;

import static aserto.directory.model.v3.ModelGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/model/v3/model.proto")
public final class MutinyModelGrpc implements io.quarkus.grpc.MutinyGrpc {

    private MutinyModelGrpc() {
    }

    public static MutinyModelStub newMutinyStub(io.grpc.Channel channel) {
        return new MutinyModelStub(channel);
    }

    public static class MutinyModelStub extends io.grpc.stub.AbstractStub<MutinyModelStub> implements io.quarkus.grpc.MutinyStub {

        private ModelGrpc.ModelStub delegateStub;

        private MutinyModelStub(io.grpc.Channel channel) {
            super(channel);
            delegateStub = ModelGrpc.newStub(channel);
        }

        private MutinyModelStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
            delegateStub = ModelGrpc.newStub(channel).build(channel, callOptions);
        }

        @Override
        protected MutinyModelStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new MutinyModelStub(channel, callOptions);
        }

        /**
         * <pre>
         *  delete manifest instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.DeleteManifestResponse> deleteManifest(aserto.directory.model.v3.ModelOuterClass.DeleteManifestRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToOne(request, delegateStub::deleteManifest);
        }

        /**
         * <pre>
         *  get manifest instance
         * </pre>
         */
        public io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.GetManifestResponse> getManifest(aserto.directory.model.v3.ModelOuterClass.GetManifestRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToMany(request, delegateStub::getManifest);
        }

        /**
         * <pre>
         *  set manifest instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.SetManifestResponse> setManifest(io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.SetManifestRequest> request) {
            return io.quarkus.grpc.stubs.ClientCalls.manyToOne(request, delegateStub::setManifest);
        }
    }

    public static abstract class ModelImplBase implements io.grpc.BindableService {

        private String compression;

        /**
         * Set whether the server will try to use a compressed response.
         *
         * @param compression the compression, e.g {@code gzip}
         */
        public ModelImplBase withCompression(String compression) {
            this.compression = compression;
            return this;
        }

        /**
         * <pre>
         *  delete manifest instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.DeleteManifestResponse> deleteManifest(aserto.directory.model.v3.ModelOuterClass.DeleteManifestRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  get manifest instance
         * </pre>
         */
        public io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.GetManifestResponse> getManifest(aserto.directory.model.v3.ModelOuterClass.GetManifestRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        /**
         * <pre>
         *  set manifest instance
         * </pre>
         */
        public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.SetManifestResponse> setManifest(io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.SetManifestRequest> request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(aserto.directory.model.v3.ModelGrpc.getGetManifestMethod(), asyncServerStreamingCall(new MethodHandlers<aserto.directory.model.v3.ModelOuterClass.GetManifestRequest, aserto.directory.model.v3.ModelOuterClass.GetManifestResponse>(this, METHODID_GET_MANIFEST, compression))).addMethod(aserto.directory.model.v3.ModelGrpc.getSetManifestMethod(), asyncClientStreamingCall(new MethodHandlers<aserto.directory.model.v3.ModelOuterClass.SetManifestRequest, aserto.directory.model.v3.ModelOuterClass.SetManifestResponse>(this, METHODID_SET_MANIFEST, compression))).addMethod(aserto.directory.model.v3.ModelGrpc.getDeleteManifestMethod(), asyncUnaryCall(new MethodHandlers<aserto.directory.model.v3.ModelOuterClass.DeleteManifestRequest, aserto.directory.model.v3.ModelOuterClass.DeleteManifestResponse>(this, METHODID_DELETE_MANIFEST, compression))).build();
        }
    }

    private static final int METHODID_GET_MANIFEST = 0;

    private static final int METHODID_SET_MANIFEST = 1;

    private static final int METHODID_DELETE_MANIFEST = 2;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final ModelImplBase serviceImpl;

        private final int methodId;

        private final String compression;

        MethodHandlers(ModelImplBase serviceImpl, int methodId, String compression) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
            this.compression = compression;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_GET_MANIFEST:
                    io.quarkus.grpc.stubs.ServerCalls.oneToMany((aserto.directory.model.v3.ModelOuterClass.GetManifestRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.model.v3.ModelOuterClass.GetManifestResponse>) responseObserver, compression, serviceImpl::getManifest);
                    break;
                case METHODID_DELETE_MANIFEST:
                    io.quarkus.grpc.stubs.ServerCalls.oneToOne((aserto.directory.model.v3.ModelOuterClass.DeleteManifestRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.model.v3.ModelOuterClass.DeleteManifestResponse>) responseObserver, compression, serviceImpl::deleteManifest);
                    break;
                default:
                    throw new java.lang.AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_SET_MANIFEST:
                    return (io.grpc.stub.StreamObserver<Req>) io.quarkus.grpc.stubs.ServerCalls.manyToOne((io.grpc.stub.StreamObserver<aserto.directory.model.v3.ModelOuterClass.SetManifestResponse>) responseObserver, serviceImpl::setManifest);
                default:
                    throw new java.lang.AssertionError();
            }
        }
    }
}
