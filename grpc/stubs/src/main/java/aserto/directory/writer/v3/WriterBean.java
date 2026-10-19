package aserto.directory.writer.v3;

import io.grpc.BindableService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.MutinyBean;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/writer/v3/writer.proto")
public class WriterBean extends MutinyWriterGrpc.WriterImplBase implements BindableService, MutinyBean {

    private final Writer delegate;

    WriterBean(@GrpcService Writer delegate) {
        this.delegate = delegate;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetObjectResponse> setObject(aserto.directory.writer.v3.SetObjectRequest request) {
        try {
            return delegate.setObject(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteObjectResponse> deleteObject(aserto.directory.writer.v3.DeleteObjectRequest request) {
        try {
            return delegate.deleteObject(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetRelationResponse> setRelation(aserto.directory.writer.v3.SetRelationRequest request) {
        try {
            return delegate.setRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteRelationResponse> deleteRelation(aserto.directory.writer.v3.DeleteRelationRequest request) {
        try {
            return delegate.deleteRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
