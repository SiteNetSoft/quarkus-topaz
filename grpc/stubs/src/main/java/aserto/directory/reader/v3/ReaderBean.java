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
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectResponse> getObject(aserto.directory.reader.v3.GetObjectRequest request) {
        try {
            return delegate.getObject(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.GetObjectManyRequest request) {
        try {
            return delegate.getObjectMany(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectsResponse> getObjects(aserto.directory.reader.v3.GetObjectsRequest request) {
        try {
            return delegate.getObjects(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationResponse> getRelation(aserto.directory.reader.v3.GetRelationRequest request) {
        try {
            return delegate.getRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationsResponse> getRelations(aserto.directory.reader.v3.GetRelationsRequest request) {
        try {
            return delegate.getRelations(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckResponse> check(aserto.directory.reader.v3.CheckRequest request) {
        try {
            return delegate.check(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ChecksResponse> checks(aserto.directory.reader.v3.ChecksRequest request) {
        try {
            return delegate.checks(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.CheckPermissionRequest request) {
        try {
            return delegate.checkPermission(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.CheckRelationRequest request) {
        try {
            return delegate.checkRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetGraphResponse> getGraph(aserto.directory.reader.v3.GetGraphRequest request) {
        try {
            return delegate.getGraph(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
