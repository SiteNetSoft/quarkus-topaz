package aserto.directory.model.v3;

import io.grpc.BindableService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.MutinyBean;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/model/v3/model.proto")
public class ModelBean extends MutinyModelGrpc.ModelImplBase implements BindableService, MutinyBean {

    private final Model delegate;

    ModelBean(@GrpcService Model delegate) {
        this.delegate = delegate;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.DeleteManifestResponse> deleteManifest(aserto.directory.model.v3.ModelOuterClass.DeleteManifestRequest request) {
        try {
            return delegate.deleteManifest(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.GetManifestResponse> getManifest(aserto.directory.model.v3.ModelOuterClass.GetManifestRequest request) {
        try {
            return delegate.getManifest(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.SetManifestResponse> setManifest(io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.SetManifestRequest> request) {
        try {
            return delegate.setManifest(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
