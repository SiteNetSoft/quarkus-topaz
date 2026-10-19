package aserto.directory.reader.v3;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ReaderGrpc {

    private ReaderGrpc() {
    }

    public static final java.lang.String SERVICE_NAME = "aserto.directory.reader.v3.Reader";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> getGetObjectMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "GetObject", requestType = aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> getGetObjectMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> getGetObjectMethod;
        if ((getGetObjectMethod = ReaderGrpc.getGetObjectMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getGetObjectMethod = ReaderGrpc.getGetObjectMethod) == null) {
                    ReaderGrpc.getGetObjectMethod = getGetObjectMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetObject")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("GetObject")).build();
                }
            }
        }
        return getGetObjectMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> getGetObjectManyMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "GetObjectMany", requestType = aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> getGetObjectManyMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> getGetObjectManyMethod;
        if ((getGetObjectManyMethod = ReaderGrpc.getGetObjectManyMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getGetObjectManyMethod = ReaderGrpc.getGetObjectManyMethod) == null) {
                    ReaderGrpc.getGetObjectManyMethod = getGetObjectManyMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetObjectMany")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("GetObjectMany")).build();
                }
            }
        }
        return getGetObjectManyMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> getGetObjectsMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "GetObjects", requestType = aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> getGetObjectsMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> getGetObjectsMethod;
        if ((getGetObjectsMethod = ReaderGrpc.getGetObjectsMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getGetObjectsMethod = ReaderGrpc.getGetObjectsMethod) == null) {
                    ReaderGrpc.getGetObjectsMethod = getGetObjectsMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetObjects")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("GetObjects")).build();
                }
            }
        }
        return getGetObjectsMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> getGetRelationMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "GetRelation", requestType = aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> getGetRelationMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> getGetRelationMethod;
        if ((getGetRelationMethod = ReaderGrpc.getGetRelationMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getGetRelationMethod = ReaderGrpc.getGetRelationMethod) == null) {
                    ReaderGrpc.getGetRelationMethod = getGetRelationMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRelation")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("GetRelation")).build();
                }
            }
        }
        return getGetRelationMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> getGetRelationsMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "GetRelations", requestType = aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> getGetRelationsMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> getGetRelationsMethod;
        if ((getGetRelationsMethod = ReaderGrpc.getGetRelationsMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getGetRelationsMethod = ReaderGrpc.getGetRelationsMethod) == null) {
                    ReaderGrpc.getGetRelationsMethod = getGetRelationsMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRelations")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("GetRelations")).build();
                }
            }
        }
        return getGetRelationsMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> getCheckMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "Check", requestType = aserto.directory.reader.v3.ReaderOuterClass.CheckRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.CheckResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> getCheckMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> getCheckMethod;
        if ((getCheckMethod = ReaderGrpc.getCheckMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getCheckMethod = ReaderGrpc.getCheckMethod) == null) {
                    ReaderGrpc.getCheckMethod = getCheckMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.CheckRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "Check")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.CheckResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("Check")).build();
                }
            }
        }
        return getCheckMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest, aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> getChecksMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "Checks", requestType = aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest, aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> getChecksMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest, aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> getChecksMethod;
        if ((getChecksMethod = ReaderGrpc.getChecksMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getChecksMethod = ReaderGrpc.getChecksMethod) == null) {
                    ReaderGrpc.getChecksMethod = getChecksMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest, aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "Checks")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("Checks")).build();
                }
            }
        }
        return getChecksMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> getCheckPermissionMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "CheckPermission", requestType = aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> getCheckPermissionMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> getCheckPermissionMethod;
        if ((getCheckPermissionMethod = ReaderGrpc.getCheckPermissionMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getCheckPermissionMethod = ReaderGrpc.getCheckPermissionMethod) == null) {
                    ReaderGrpc.getCheckPermissionMethod = getCheckPermissionMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckPermission")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("CheckPermission")).build();
                }
            }
        }
        return getCheckPermissionMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> getCheckRelationMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "CheckRelation", requestType = aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> getCheckRelationMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> getCheckRelationMethod;
        if ((getCheckRelationMethod = ReaderGrpc.getCheckRelationMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getCheckRelationMethod = ReaderGrpc.getCheckRelationMethod) == null) {
                    ReaderGrpc.getCheckRelationMethod = getCheckRelationMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckRelation")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("CheckRelation")).build();
                }
            }
        }
        return getCheckRelationMethod;
    }

    private static volatile io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest, aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> getGetGraphMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "GetGraph", requestType = aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest.class, responseType = aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest, aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> getGetGraphMethod() {
        io.grpc.MethodDescriptor<aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest, aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> getGetGraphMethod;
        if ((getGetGraphMethod = ReaderGrpc.getGetGraphMethod) == null) {
            synchronized (ReaderGrpc.class) {
                if ((getGetGraphMethod = ReaderGrpc.getGetGraphMethod) == null) {
                    ReaderGrpc.getGetGraphMethod = getGetGraphMethod = io.grpc.MethodDescriptor.<aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest, aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetGraph")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse.getDefaultInstance())).setSchemaDescriptor(new ReaderMethodDescriptorSupplier("GetGraph")).build();
                }
            }
        }
        return getGetGraphMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static ReaderStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ReaderStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ReaderStub>() {

            @java.lang.Override
            public ReaderStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ReaderStub(channel, callOptions);
            }
        };
        return ReaderStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports all types of calls on the service
     */
    public static ReaderBlockingV2Stub newBlockingV2Stub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ReaderBlockingV2Stub> factory = new io.grpc.stub.AbstractStub.StubFactory<ReaderBlockingV2Stub>() {

            @java.lang.Override
            public ReaderBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ReaderBlockingV2Stub(channel, callOptions);
            }
        };
        return ReaderBlockingV2Stub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static ReaderBlockingStub newBlockingStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ReaderBlockingStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ReaderBlockingStub>() {

            @java.lang.Override
            public ReaderBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ReaderBlockingStub(channel, callOptions);
            }
        };
        return ReaderBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static ReaderFutureStub newFutureStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ReaderFutureStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ReaderFutureStub>() {

            @java.lang.Override
            public ReaderFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ReaderFutureStub(channel, callOptions);
            }
        };
        return ReaderFutureStub.newStub(factory, channel);
    }

    /**
     */
    public interface AsyncService {

        /**
         * <pre>
         * get object
         * </pre>
         */
        default void getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetObjectMethod(), responseObserver);
        }

        /**
         * <pre>
         * get multiple objects
         * </pre>
         */
        default void getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetObjectManyMethod(), responseObserver);
        }

        /**
         * <pre>
         * list objects
         * </pre>
         */
        default void getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetObjectsMethod(), responseObserver);
        }

        /**
         * <pre>
         * get relation
         * </pre>
         */
        default void getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRelationMethod(), responseObserver);
        }

        /**
         * <pre>
         * list relations
         * </pre>
         */
        default void getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRelationsMethod(), responseObserver);
        }

        /**
         * <pre>
         * check if subject has relation or permission with object
         * </pre>
         */
        default void check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckMethod(), responseObserver);
        }

        /**
         * <pre>
         * checks validates a set of check requests in a single roundtrip
         * </pre>
         */
        default void checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getChecksMethod(), responseObserver);
        }

        /**
         * <pre>
         * check permission (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        default void checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckPermissionMethod(), responseObserver);
        }

        /**
         * <pre>
         * check relation (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        default void checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckRelationMethod(), responseObserver);
        }

        /**
         * <pre>
         * get object relationship graph
         * </pre>
         */
        default void getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetGraphMethod(), responseObserver);
        }
    }

    /**
     * Base class for the server implementation of the service Reader.
     */
    public static abstract class ReaderImplBase implements io.grpc.BindableService, AsyncService {

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return ReaderGrpc.bindService(this);
        }
    }

    /**
     * A stub to allow clients to do asynchronous rpc calls to service Reader.
     */
    public static class ReaderStub extends io.grpc.stub.AbstractAsyncStub<ReaderStub> {

        private ReaderStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ReaderStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ReaderStub(channel, callOptions);
        }

        /**
         * <pre>
         * get object
         * </pre>
         */
        public void getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getGetObjectMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * get multiple objects
         * </pre>
         */
        public void getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getGetObjectManyMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * list objects
         * </pre>
         */
        public void getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getGetObjectsMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * get relation
         * </pre>
         */
        public void getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getGetRelationMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * list relations
         * </pre>
         */
        public void getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getGetRelationsMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * check if subject has relation or permission with object
         * </pre>
         */
        public void check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getCheckMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * checks validates a set of check requests in a single roundtrip
         * </pre>
         */
        public void checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getChecksMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * check permission (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public void checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getCheckPermissionMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * check relation (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public void checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getCheckRelationMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * get object relationship graph
         * </pre>
         */
        public void getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request, io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getGetGraphMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     * A stub to allow clients to do synchronous rpc calls to service Reader.
     */
    public static class ReaderBlockingV2Stub extends io.grpc.stub.AbstractBlockingStub<ReaderBlockingV2Stub> {

        private ReaderBlockingV2Stub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ReaderBlockingV2Stub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ReaderBlockingV2Stub(channel, callOptions);
        }

        /**
         * <pre>
         * get object
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getGetObjectMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * get multiple objects
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getGetObjectManyMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * list objects
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getGetObjectsMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * get relation
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getGetRelationMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * list relations
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getGetRelationsMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * check if subject has relation or permission with object
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.CheckResponse check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getCheckMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * checks validates a set of check requests in a single roundtrip
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getChecksMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * check permission (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getCheckPermissionMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * check relation (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getCheckRelationMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * get object relationship graph
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request) throws io.grpc.StatusException {
            return io.grpc.stub.ClientCalls.blockingV2UnaryCall(getChannel(), getGetGraphMethod(), getCallOptions(), request);
        }
    }

    /**
     * A stub to allow clients to do limited synchronous rpc calls to service Reader.
     */
    public static class ReaderBlockingStub extends io.grpc.stub.AbstractBlockingStub<ReaderBlockingStub> {

        private ReaderBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ReaderBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ReaderBlockingStub(channel, callOptions);
        }

        /**
         * <pre>
         * get object
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getGetObjectMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * get multiple objects
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getGetObjectManyMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * list objects
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getGetObjectsMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * get relation
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getGetRelationMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * list relations
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getGetRelationsMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * check if subject has relation or permission with object
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.CheckResponse check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getCheckMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * checks validates a set of check requests in a single roundtrip
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getChecksMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * check permission (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getCheckPermissionMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * check relation (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getCheckRelationMethod(), getCallOptions(), request);
        }

        /**
         * <pre>
         * get object relationship graph
         * </pre>
         */
        public aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getGetGraphMethod(), getCallOptions(), request);
        }
    }

    /**
     * A stub to allow clients to do ListenableFuture-style rpc calls to service Reader.
     */
    public static class ReaderFutureStub extends io.grpc.stub.AbstractFutureStub<ReaderFutureStub> {

        private ReaderFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected ReaderFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ReaderFutureStub(channel, callOptions);
        }

        /**
         * <pre>
         * get object
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getGetObjectMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * get multiple objects
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getGetObjectManyMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * list objects
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getGetObjectsMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * get relation
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getGetRelationMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * list relations
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getGetRelationsMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * check if subject has relation or permission with object
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getCheckMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * checks validates a set of check requests in a single roundtrip
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getChecksMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * check permission (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getCheckPermissionMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * check relation (deprecated, use the check method)
         * Deprecated: use directory.reader.v3.Check()
         * </pre>
         */
        @java.lang.Deprecated
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getCheckRelationMethod(), getCallOptions()), request);
        }

        /**
         * <pre>
         * get object relationship graph
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getGetGraphMethod(), getCallOptions()), request);
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
                case METHODID_GET_OBJECT:
                    serviceImpl.getObject((aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse>) responseObserver);
                    break;
                case METHODID_GET_OBJECT_MANY:
                    serviceImpl.getObjectMany((aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse>) responseObserver);
                    break;
                case METHODID_GET_OBJECTS:
                    serviceImpl.getObjects((aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse>) responseObserver);
                    break;
                case METHODID_GET_RELATION:
                    serviceImpl.getRelation((aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse>) responseObserver);
                    break;
                case METHODID_GET_RELATIONS:
                    serviceImpl.getRelations((aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse>) responseObserver);
                    break;
                case METHODID_CHECK:
                    serviceImpl.check((aserto.directory.reader.v3.ReaderOuterClass.CheckRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckResponse>) responseObserver);
                    break;
                case METHODID_CHECKS:
                    serviceImpl.checks((aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse>) responseObserver);
                    break;
                case METHODID_CHECK_PERMISSION:
                    serviceImpl.checkPermission((aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse>) responseObserver);
                    break;
                case METHODID_CHECK_RELATION:
                    serviceImpl.checkRelation((aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse>) responseObserver);
                    break;
                case METHODID_GET_GRAPH:
                    serviceImpl.getGraph((aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest) request, (io.grpc.stub.StreamObserver<aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse>) responseObserver);
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
        return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(getGetObjectMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse>(service, METHODID_GET_OBJECT))).addMethod(getGetObjectManyMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse>(service, METHODID_GET_OBJECT_MANY))).addMethod(getGetObjectsMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse>(service, METHODID_GET_OBJECTS))).addMethod(getGetRelationMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse>(service, METHODID_GET_RELATION))).addMethod(getGetRelationsMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest, aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse>(service, METHODID_GET_RELATIONS))).addMethod(getCheckMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.CheckRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckResponse>(service, METHODID_CHECK))).addMethod(getChecksMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest, aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse>(service, METHODID_CHECKS))).addMethod(getCheckPermissionMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse>(service, METHODID_CHECK_PERMISSION))).addMethod(getCheckRelationMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest, aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse>(service, METHODID_CHECK_RELATION))).addMethod(getGetGraphMethod(), io.grpc.stub.ServerCalls.asyncUnaryCall(new MethodHandlers<aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest, aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse>(service, METHODID_GET_GRAPH))).build();
    }

    private static abstract class ReaderBaseDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {

        ReaderBaseDescriptorSupplier() {
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return aserto.directory.reader.v3.ReaderOuterClass.getDescriptor();
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("Reader");
        }
    }

    private static final class ReaderFileDescriptorSupplier extends ReaderBaseDescriptorSupplier {

        ReaderFileDescriptorSupplier() {
        }
    }

    private static final class ReaderMethodDescriptorSupplier extends ReaderBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {

        private final java.lang.String methodName;

        ReaderMethodDescriptorSupplier(java.lang.String methodName) {
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
            synchronized (ReaderGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME).setSchemaDescriptor(new ReaderFileDescriptorSupplier()).addMethod(getGetObjectMethod()).addMethod(getGetObjectManyMethod()).addMethod(getGetObjectsMethod()).addMethod(getGetRelationMethod()).addMethod(getGetRelationsMethod()).addMethod(getCheckMethod()).addMethod(getChecksMethod()).addMethod(getCheckPermissionMethod()).addMethod(getCheckRelationMethod()).addMethod(getGetGraphMethod()).build();
                }
            }
        }
        return result;
    }
}
