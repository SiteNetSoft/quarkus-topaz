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
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> setObject(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest request) {
        try {
            return delegate.setObject(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> deleteObject(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest request) {
        try {
            return delegate.deleteObject(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> setRelation(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest request) {
        try {
            return delegate.setRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> deleteRelation(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest request) {
        try {
            return delegate.deleteRelation(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
