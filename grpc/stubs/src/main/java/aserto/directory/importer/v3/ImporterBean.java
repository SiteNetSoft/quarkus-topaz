package aserto.directory.importer.v3;

import io.grpc.BindableService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.MutinyBean;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/importer/v3/importer.proto")
public class ImporterBean extends MutinyImporterGrpc.ImporterImplBase implements BindableService, MutinyBean {

    private final Importer delegate;

    ImporterBean(@GrpcService Importer delegate) {
        this.delegate = delegate;
    }

    @Override
    public io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImporterOuterClass.ImportResponse> import_(io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImporterOuterClass.ImportRequest> request) {
        try {
            return delegate.import_(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
