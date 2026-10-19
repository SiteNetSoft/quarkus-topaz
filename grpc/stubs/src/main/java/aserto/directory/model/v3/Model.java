package aserto.directory.model.v3;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/model/v3/model.proto")
public interface Model extends MutinyService {

    /**
     * <pre>
     *  delete manifest instance
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.DeleteManifestResponse> deleteManifest(aserto.directory.model.v3.ModelOuterClass.DeleteManifestRequest request);

    /**
     * <pre>
     *  get manifest instance
     * </pre>
     */
    io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.GetManifestResponse> getManifest(aserto.directory.model.v3.ModelOuterClass.GetManifestRequest request);

    /**
     * <pre>
     *  set manifest instance
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.SetManifestResponse> setManifest(io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.SetManifestRequest> request);
}
