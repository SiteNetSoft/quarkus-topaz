package aserto.directory.exporter.v3;

import static aserto.directory.exporter.v3.ExporterGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/exporter/v3/exporter.proto")
public final class MutinyExporterGrpc implements io.quarkus.grpc.MutinyGrpc {

    private MutinyExporterGrpc() {
    }

    public static MutinyExporterStub newMutinyStub(io.grpc.Channel channel) {
        return new MutinyExporterStub(channel);
    }

    public static class MutinyExporterStub extends io.grpc.stub.AbstractStub<MutinyExporterStub> implements io.quarkus.grpc.MutinyStub {

        private ExporterGrpc.ExporterStub delegateStub;

        private MutinyExporterStub(io.grpc.Channel channel) {
            super(channel);
            delegateStub = ExporterGrpc.newStub(channel);
        }

        private MutinyExporterStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
            delegateStub = ExporterGrpc.newStub(channel).build(channel, callOptions);
        }

        @Override
        protected MutinyExporterStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new MutinyExporterStub(channel, callOptions);
        }

        /**
         * <pre>
         *  export objects and relations as a stream
         * </pre>
         */
        public io.smallrye.mutiny.Multi<aserto.directory.exporter.v3.ExportResponse> export(aserto.directory.exporter.v3.ExportRequest request) {
            return io.quarkus.grpc.stubs.ClientCalls.oneToMany(request, delegateStub::export);
        }
    }

    public static abstract class ExporterImplBase implements io.grpc.BindableService {

        private String compression;

        /**
         * Set whether the server will try to use a compressed response.
         *
         * @param compression the compression, e.g {@code gzip}
         */
        public ExporterImplBase withCompression(String compression) {
            this.compression = compression;
            return this;
        }

        /**
         * <pre>
         *  export objects and relations as a stream
         * </pre>
         */
        public io.smallrye.mutiny.Multi<aserto.directory.exporter.v3.ExportResponse> export(aserto.directory.exporter.v3.ExportRequest request) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(aserto.directory.exporter.v3.ExporterGrpc.getExportMethod(), asyncServerStreamingCall(new MethodHandlers<aserto.directory.exporter.v3.ExportRequest, aserto.directory.exporter.v3.ExportResponse>(this, METHODID_EXPORT, compression))).build();
        }
    }

    private static final int METHODID_EXPORT = 0;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final ExporterImplBase serviceImpl;

        private final int methodId;

        private final String compression;

        MethodHandlers(ExporterImplBase serviceImpl, int methodId, String compression) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
            this.compression = compression;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_EXPORT:
                    io.quarkus.grpc.stubs.ServerCalls.oneToMany((aserto.directory.exporter.v3.ExportRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.exporter.v3.ExportResponse>) responseObserver, compression, serviceImpl::export);
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
