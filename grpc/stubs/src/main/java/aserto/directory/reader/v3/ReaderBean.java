package aserto.directory.reader.v3;

import io.grpc.BindableService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.MutinyBean;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/reader/v3/reader.proto")
public class ReaderBean extends MutinyReaderGrpc.ReaderImplBase implements BindableService, MutinyBean {

    private final Reader delegate;

    ReaderBean(@GrpcService Reader delegate) {
        this.delegate = delegate;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request) {
        try {
            return delegate.getObject(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request) {
        try {
            return delegate.getObjectMany(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request) {
        try {
            return delegate.getObjects(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request) {
        try {
            return delegate.getRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request) {
        try {
            return delegate.getRelations(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request) {
        try {
            return delegate.check(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request) {
        try {
            return delegate.checks(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request) {
        try {
            return delegate.checkPermission(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request) {
        try {
            return delegate.checkRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request) {
        try {
            return delegate.getGraph(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
