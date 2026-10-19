package aserto.directory.writer.v3;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class WriterGrpc {

    private WriterGrpc() {
    }

    public static final java.lang.String SERVICE_NAME = "aserto.directory.writer.v3.Writer";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest, aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> getSetObjectMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "SetObject", requestType = aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest.class, responseType = aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest, aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> getSetObjectMethod() {
        io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest, aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> getSetObjectMethod;
        if ((getSetObjectMethod = WriterGrpc.getSetObjectMethod) == null) {
            synchronized (WriterGrpc.class) {
                if ((getSetObjectMethod = WriterGrpc.getSetObjectMethod) == null) {
                    WriterGrpc.getSetObjectMethod = getSetObjectMethod = io.grpc.MethodDescriptor.<aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest, aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetObject")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse.getDefaultInstance())).setSchemaDescriptor(new WriterMethodDescriptorSupplier("SetObject")).build();
                }
            }
        }
        return getSetObjectMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> getDeleteObjectMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "DeleteObject", requestType = aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest.class, responseType = aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> getDeleteObjectMethod() {
        io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> getDeleteObjectMethod;
        if ((getDeleteObjectMethod = WriterGrpc.getDeleteObjectMethod) == null) {
            synchronized (WriterGrpc.class) {
                if ((getDeleteObjectMethod = WriterGrpc.getDeleteObjectMethod) == null) {
                    WriterGrpc.getDeleteObjectMethod = getDeleteObjectMethod = io.grpc.MethodDescriptor.<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteObject")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse.getDefaultInstance())).setSchemaDescriptor(new WriterMethodDescriptorSupplier("DeleteObject")).build();
                }
            }
        }
        return getDeleteObjectMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest, aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> getSetRelationMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "SetRelation", requestType = aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest.class, responseType = aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest, aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> getSetRelationMethod() {
        io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest, aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> getSetRelationMethod;
        if ((getSetRelationMethod = WriterGrpc.getSetRelationMethod) == null) {
            synchronized (WriterGrpc.class) {
                if ((getSetRelationMethod = WriterGrpc.getSetRelationMethod) == null) {
                    WriterGrpc.getSetRelationMethod = getSetRelationMethod = io.grpc.MethodDescriptor.<aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest, aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetRelation")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse.getDefaultInstance())).setSchemaDescriptor(new WriterMethodDescriptorSupplier("SetRelation")).build();
                }
            }
        }
        return getSetRelationMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> getDeleteRelationMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "DeleteRelation", requestType = aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest.class, responseType = aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> getDeleteRelationMethod() {
        io.grpc.MethodDescriptor<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> getDeleteRelationMethod;
        if ((getDeleteRelationMethod = WriterGrpc.getDeleteRelationMethod) == null) {
            synchronized (WriterGrpc.class) {
                if ((getDeleteRelationMethod = WriterGrpc.getDeleteRelationMethod) == null) {
                    WriterGrpc.getDeleteRelationMethod = getDeleteRelationMethod = io.grpc.MethodDescriptor.<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteRelation")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse.getDefaultInstance())).setSchemaDescriptor(new WriterMethodDescriptorSupplier("DeleteRelation")).build();
                }
            }
        }
        return getDeleteRelationMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static WriterStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<WriterStub> factory = new io.grpc.stub.AbstractStub.StubFactory<WriterStub>() {

            @java.lang.Override
            public WriterStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new WriterStub(channel, callOptions);
            }
        };
        return WriterStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports all types of calls on the service
     */
    public static WriterBlockingV2Stub newBlockingV2Stub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<WriterBlockingV2Stub> factory = new io.grpc.stub.AbstractStub.StubFactory<WriterBlockingV2Stub>() {

            @java.lang.Override
            public WriterBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new WriterBlockingV2Stub(channel, callOptions);
            }
        };
        return WriterBlockingV2Stub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static WriterBlockingStub newBlockingStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<WriterBlockingStub> factory = new io.grpc.stub.AbstractStub.StubFactory<WriterBlockingStub>() {

            @java.lang.Override
            public WriterBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new WriterBlockingStub(channel, callOptions);
            }
        };
        return WriterBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static WriterFutureStub newFutureStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<WriterFutureStub> factory = new io.grpc.stub.AbstractStub.StubFactory<WriterFutureStub>() {

            @java.lang.Override
            public WriterFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new WriterFutureStub(channel, callOptions);
            }
        };
        return WriterFutureStub.newStub(factory, channel);
    }

    /**
     */
    public interface AsyncService {

        /**
         * <pre>
         * set object instance
         * </pre>
         */
        default void setObject(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetObjectMethod(), responseObserver);
        }

        /**
         * <pre>
         * delete object instance
         * </pre>
         */
        default void deleteObject(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteObjectMethod(), responseObserver);
        }

        /**
         * <pre>
         * set relation instance
         * </pre>
         */
        default void setRelation(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetRelationMethod(), responseObserver);
        }

        /**
         * <pre>
         * delete relation instance
         * </pre>
         */
        default void deleteRelation(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteRelationMethod(), responseObserver);
        }
    }

    /**
     * Base class for the server implementation of the service Writer.
     */
    public static abstract class WriterImplBase implements io.grpc.BindableService, AsyncService {

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return WriterGrpc.bindService(this);
        }
    }

    /**
     * A stub to allow clients to do asynchronous rpc calls to service Writer.
     */
    public static class WriterStub extends io.grpc.stub.AbstractAsyncStub<WriterStub> {

        private WriterStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected WriterStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new WriterStub(channel, callOptions);
        }

        /**
         * <pre>
         * set object instance
         * </pre>
         */
        public void setObject(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getSetObjectMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * delete object instance
         * </pre>
         */
        public void deleteObject(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getDeleteObjectMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * set relation instance
         * </pre>
         */
        public void setRelation(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getSetRelationMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * delete relation instance
         * </pre>
         */
        public void deleteRelation(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getDeleteRelationMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     * A stub to allow clients to do synchronous rpc calls to service Writer.
     */
    public static class WriterBlockingV2Stub extends io.grpc.stub.AbstractBlockingStub<WriterBlockingV2Stub> {

        private WriterBlockingV2Stub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected WriterBlockingV2Stub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new WriterBlockingV2Stub(channel, callOptions);
        }

        /**
         * <pre>
         * set object instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse setObject(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getSetObjectMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * delete object instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse deleteObject(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getDeleteObjectMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * set relation instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse setRelation(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getSetRelationMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * delete relation instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse deleteRelation(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getDeleteRelationMethod(), getCallOptions(), request);
        }
    }

    /**
     * A stub to allow clients to do limited synchronous rpc calls to service Writer.
     */
    public static class WriterBlockingStub extends io.grpc.stub.AbstractBlockingStub<WriterBlockingStub> {

        private WriterBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected WriterBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new WriterBlockingStub(channel, callOptions);
        }

        /**
         * <pre>
         * set object instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse setObject(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getSetObjectMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * delete object instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse deleteObject(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getDeleteObjectMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * set relation instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse setRelation(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getSetRelationMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * delete relation instance
         * </pre>
         */
        public aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse deleteRelation(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getDeleteRelationMethod(), getCallOptions(), request);
        }
    }

    /**
     * A stub to allow clients to do ListenableFuture-style rpc calls to service Writer.
     */
    public static class WriterFutureStub extends io.grpc.stub.AbstractFutureStub<WriterFutureStub> {

        private WriterFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected WriterFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new WriterFutureStub(channel, callOptions);
        }

        /**
         * <pre>
         * set object instance
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> setObject(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getSetObjectMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * delete object instance
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> deleteObject(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getDeleteObjectMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * set relation instance
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> setRelation(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getSetRelationMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * delete relation instance
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> deleteRelation(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getDeleteRelationMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_SET_OBJECT = 0;

    private static final int METHODID_DELETE_OBJECT = 1;

    private static final int METHODID_SET_RELATION = 2;

    private static final int METHODID_DELETE_RELATION = 3;

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
                case METHODID_SET_OBJECT:
                    serviceImpl.setObject((aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse>) responseObserver);
                    break;
                case METHODID_DELETE_OBJECT:
                    serviceImpl.deleteObject((aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse>) responseObserver);
                    break;
                case METHODID_SET_RELATION:
                    serviceImpl.setRelation((aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse>) responseObserver);
                    break;
                case METHODID_DELETE_RELATION:
                    serviceImpl.deleteRelation((aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse>) responseObserver);
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
        return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(getSetObjectMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest, aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse>(service, METHODID_SET_OBJECT))).addMethod(getDeleteObjectMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse>(service, METHODID_DELETE_OBJECT))).addMethod(getSetRelationMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest, aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse>(service, METHODID_SET_RELATION))).addMethod(getDeleteRelationMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest, aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse>(service, METHODID_DELETE_RELATION))).build();
    }

    private static abstract class WriterBaseDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {

        WriterBaseDescriptorSupplier() {
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return aserto.directory.writer.v3.WriterOuterClass.getDescriptor();
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("Writer");
        }
    }

    private static final class WriterFileDescriptorSupplier extends WriterBaseDescriptorSupplier {

        WriterFileDescriptorSupplier() {
        }
    }

    private static final class WriterMethodDescriptorSupplier extends WriterBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {

        private final java.lang.String methodName;

        WriterMethodDescriptorSupplier(java.lang.String methodName) {
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
            synchronized (WriterGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME).setSchemaDescriptor(new WriterFileDescriptorSupplier()).addMethod(getSetObjectMethod()).addMethod(getDeleteObjectMethod()).addMethod(getSetRelationMethod()).addMethod(getDeleteRelationMethod()).build();
                }
            }
        }
        return result;
    }
}
