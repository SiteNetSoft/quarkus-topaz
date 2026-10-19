package aserto.directory.exporter.v3;

import io.grpc.BindableService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.MutinyBean;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/exporter/v3/exporter.proto")
public class ExporterBean extends MutinyExporterGrpc.ExporterImplBase implements BindableService, MutinyBean {

    private final Exporter delegate;

    ExporterBean(@GrpcService Exporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public io.smallrye.mutiny.Multi<aserto.directory.exporter.v3.ExporterOuterClass.ExportResponse> export(aserto.directory.exporter.v3.ExporterOuterClass.ExportRequest request) {
        try {
            return delegate.export(request);
        } catch (UnsupportedOperationException e) {
            throw new io.grpc.StatusRuntimeException(io.grpc.Status.UNIMPLEMENTED);
        }
    }
}
