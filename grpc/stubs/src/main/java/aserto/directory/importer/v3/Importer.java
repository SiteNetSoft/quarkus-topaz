package aserto.directory.importer.v3;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/importer/v3/importer.proto")
public interface Importer extends MutinyService {

    /**
     * <pre>
     *  import stream of objects and relations
     * </pre>
     */
    io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImporterOuterClass.ImportResponse> import_(io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImporterOuterClass.ImportRequest> request);
}
