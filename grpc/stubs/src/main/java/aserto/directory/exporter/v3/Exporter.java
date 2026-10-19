package aserto.directory.exporter.v3;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/exporter/v3/exporter.proto")
public interface Exporter extends MutinyService {

    /**
     * <pre>
     *  export objects and relations as a stream
     * </pre>
     */
    io.smallrye.mutiny.Multi<aserto.directory.exporter.v3.ExportResponse> export(aserto.directory.exporter.v3.ExportRequest request);
}
