package aserto.directory.exporter.v3;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ExporterGrpc {

    private ExporterGrpc() {
    }

    public static final java.lang.String SERVICE_NAME = "aserto.directory.exporter.v3.Exporter";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest, aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> getExportMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "Export", requestType = aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest.class, responseType = aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
    public static io.grpc.MethodDescriptor<aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest, aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> getExportMethod() {
        io.grpc.MethodDescriptor<aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest, aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> getExportMethod;
        if ((getExportMethod = ExporterGrpc.getExportMethod) == null) {
            synchronized (ExporterGrpc.class) {
                if ((getExportMethod = ExporterGrpc.getExportMethod) == null) {
                    ExporterGrpc.getExportMethod = getExportMethod = io.grpc.MethodDescriptor.<aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest, aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING).setFullMethodName(generateFullMethodName(SERVICE_NAME, "Export")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse.getDefaultInstance())).setSchemaDescriptor(new ExporterMethodDescriptorSupplier("Export")).build();
                }
            }
        }
        return getExportMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static ExporterStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ExporterStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ExporterStub>() {

            @java.lang.Override
            public ExporterStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ExporterStub(channel, callOptions);
            }
        };
        return ExporterStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports all types of calls on the service
     */
    public static ExporterBlockingV2Stub newBlockingV2Stub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ExporterBlockingV2Stub> factory = new io.grpc.stub.AbstractStub.StubFactory<ExporterBlockingV2Stub>() {

            @java.lang.Override
            public ExporterBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ExporterBlockingV2Stub(channel, callOptions);
            }
        };
        return ExporterBlockingV2Stub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static ExporterBlockingStub newBlockingStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ExporterBlockingStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ExporterBlockingStub>() {

            @java.lang.Override
            public ExporterBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ExporterBlockingStub(channel, callOptions);
            }
        };
        return ExporterBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static ExporterFutureStub newFutureStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ExporterFutureStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ExporterFutureStub>() {

            @java.lang.Override
            public ExporterFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ExporterFutureStub(channel, callOptions);
            }
        };
        return ExporterFutureStub.newStub(factory, channel);
    }

    /**
     */
    public interface AsyncService {

        /**
         * <pre>
         * export objects and relations as a stream
         * </pre>
         */
        default void export(aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest request, io.grpc.stub.StreamObserver<aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExportMethod(), responseObserver);
        }
    }

    /**
     * Base class for the server implementation of the service Exporter.
     */
    public static abstract class ExporterImplBase implements io.grpc.BindableService, AsyncService {

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return ExporterGrpc.bindService(this);
        }
    }

    /**
     * A stub to allow clients to do asynchronous rpc calls to service Exporter.
     */
    public static class ExporterStub extends io.grpc.stub.AbstractAsyncStub<ExporterStub> {

        private ExporterStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ExporterStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ExporterStub(channel, callOptions);
        }

        /**
         * <pre>
         * export objects and relations as a stream
         * </pre>
         */
        public void export(aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest request, io.grpc.stub.StreamObserver<aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncServerStreamingCall(getChannel().newCall(getExportMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     * A stub to allow clients to do synchronous rpc calls to service Exporter.
     */
    public static class ExporterBlockingV2Stub extends io.grpc.stub.AbstractBlockingStub<ExporterBlockingV2Stub> {

        private ExporterBlockingV2Stub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ExporterBlockingV2Stub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ExporterBlockingV2Stub(channel, callOptions);
        }

        /**
         * <pre>
         * export objects and relations as a stream
         * </pre>
         */
        @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
        public io.grpc.stub.BlockingClientCall<?, aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> export(aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest request) {
            return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(getChannel(), getExportMethod(), getCallOptions(), request);
        }
    }

    /**
     * A stub to allow clients to do limited synchronous rpc calls to service Exporter.
     */
    public static class ExporterBlockingStub extends io.grpc.stub.AbstractBlockingStub<ExporterBlockingStub> {

        private ExporterBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ExporterBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ExporterBlockingStub(channel, callOptions);
        }

        /**
         * <pre>
         * export objects and relations as a stream
         * </pre>
         */
        public java.util.Iterator<aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> export(aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest request) {
            return io.grpc.stub.ClientCalls.blockingServerStreamingCall(getChannel(), getExportMethod(), getCallOptions(), request);
        }
    }

    /**
     * A stub to allow clients to do ListenableFuture-style rpc calls to service Exporter.
     */
    public static class ExporterFutureStub extends io.grpc.stub.AbstractFutureStub<ExporterFutureStub> {

        private ExporterFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ExporterFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ExporterFutureStub(channel, callOptions);
        }
    }

    private static final int METHODID_EXPORT = 0;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final AsyncService serviceImpl;

        private final int methodId;

        MethodHandlers(AsyncService serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_EXPORT:
                    serviceImpl.export((aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }

    public static io.grpc.ServerServiceDefinition bindService(AsyncService service) {
        return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(getExportMethod(), io.grpc.stub.ServerCalls.asyncServerStreamingCall(new MethodHandlers<aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest, aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse>(service, METHODID_EXPORT))).build();
    }

    private static abstract class ExporterBaseDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {

        ExporterBaseDescriptorSupplier() {
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return aserto.directory.exporter.v3.ExporterOuterClass.getDescriptor();
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("Exporter");
        }
    }

    private static final class ExporterFileDescriptorSupplier extends ExporterBaseDescriptorSupplier {

        ExporterFileDescriptorSupplier() {
        }
    }

    private static final class ExporterMethodDescriptorSupplier extends ExporterBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {

        private final java.lang.String methodName;

        ExporterMethodDescriptorSupplier(java.lang.String methodName) {
            this.methodName = methodName;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }

    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (ExporterGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME).setSchemaDescriptor(new ExporterFileDescriptorSupplier()).addMethod(getExportMethod()).build();
                }
            }
        }
        return result;
    }
}
