package aserto.directory.importer.v3;

import static aserto.directory.importer.v3.ImporterGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/importer/v3/importer.proto")
public final class MutinyImporterGrpc implements io.quarkus.grpc.MutinyGrpc {

    private MutinyImporterGrpc() {
    }

    public static MutinyImporterStub newMutinyStub(io.grpc.Channel channel) {
        return new MutinyImporterStub(channel);
    }

    public static class MutinyImporterStub extends io.grpc.stub.AbstractStub<MutinyImporterStub> implements io.quarkus.grpc.MutinyStub {

        private ImporterGrpc.ImporterStub delegateStub;

        private MutinyImporterStub(io.grpc.Channel channel) {
            super(channel);
            delegateStub = ImporterGrpc.newStub(channel);
        }

        private MutinyImporterStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
            delegateStub = ImporterGrpc.newStub(channel).build(channel, callOptions);
        }

        @Override
        protected MutinyImporterStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new MutinyImporterStub(channel, callOptions);
        }

        /**
         * <pre>
         *  import stream of objects and relations
         * </pre>
         */
        public io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImportResponse> import_(io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImportRequest> request) {
            return io.quarkus.grpc.stubs.ClientCalls.manyToMany(request, delegateStub::import_);
        }
    }

    public static abstract class ImporterImplBase implements io.grpc.BindableService {

        private String compression;

        /**
         * Set whether the server will try to use a compressed response.
         *
         * @param compression the compression, e.g {@code gzip}
         */
        public ImporterImplBase withCompression(String compression) {
            this.compression = compression;
            return this;
        }

        /**
         * <pre>
         *  import stream of objects and relations
         * </pre>
         */
        public io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImportResponse> import_(io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImportRequest> request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(aserto.directory.importer.v3.ImporterGrpc.getImportMethod(), asyncBidiStreamingCall(new MethodHandlers<aserto.directory.importer.v3.ImportRequest, aserto.directory.importer.v3.ImportResponse>(this, METHODID_IMPORT_, compression))).build();
        }
    }

    private static final int METHODID_IMPORT_ = 0;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final ImporterImplBase serviceImpl;

        private final int methodId;

        private final String compression;

        MethodHandlers(ImporterImplBase serviceImpl, int methodId, String compression) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
            this.compression = compression;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                default:
                    throw new java.lang.AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_IMPORT_:
                    return (io.grpc.stub.StreamObserver<Req>) io.quarkus.grpc.stubs.ServerCalls.manyToMany((io.grpc.stub.StreamObserver<aserto.directory.importer.v3.ImportResponse>) responseObserver, serviceImpl::import_);
                default:
                    throw new java.lang.AssertionError();
            }
        }
    }
}
