package aserto.directory.reader.v3;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/reader/v3/reader.proto")
public interface Reader extends MutinyService {

    /**
     * <pre>
     *  get object
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectResponse> getObject(aserto.directory.reader.v3.GetObjectRequest request);

    /**
     * <pre>
     *  get multiple objects
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.GetObjectManyRequest request);

    /**
     * <pre>
     *  list objects
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectsResponse> getObjects(aserto.directory.reader.v3.GetObjectsRequest request);

    /**
     * <pre>
     *  get relation
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationResponse> getRelation(aserto.directory.reader.v3.GetRelationRequest request);

    /**
     * <pre>
     *  list relations
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationsResponse> getRelations(aserto.directory.reader.v3.GetRelationsRequest request);

    /**
     * <pre>
     *  check if subject has relation or permission with object
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckResponse> check(aserto.directory.reader.v3.CheckRequest request);

    /**
     * <pre>
     *  checks validates a set of check requests in a single roundtrip
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ChecksResponse> checks(aserto.directory.reader.v3.ChecksRequest request);

    /**
     * <pre>
     *  check permission (deprecated, use the check method)
     *  Deprecated: use directory.reader.v3.Check()
     * </pre>
     */
    @Deprecated
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.CheckPermissionRequest request);

    /**
     * <pre>
     *  check relation (deprecated, use the check method)
     *  Deprecated: use directory.reader.v3.Check()
     * </pre>
     */
    @Deprecated
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.CheckRelationRequest request);

    /**
     * <pre>
     *  get object relationship graph
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetGraphResponse> getGraph(aserto.directory.reader.v3.GetGraphRequest request);
}
