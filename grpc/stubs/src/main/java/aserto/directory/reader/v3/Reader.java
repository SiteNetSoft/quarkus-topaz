package aserto.directory.reader.v3;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/reader/v3/reader.proto")
public interface Reader extends MutinyService {

    /**
     * <pre>
     *  get object
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request);

    /**
     * <pre>
     *  get multiple objects
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request);

    /**
     * <pre>
     *  list objects
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request);

    /**
     * <pre>
     *  get relation
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request);

    /**
     * <pre>
     *  list relations
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request);

    /**
     * <pre>
     *  check if subject has relation or permission with object
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request);

    /**
     * <pre>
     *  checks validates a set of check requests in a single roundtrip
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request);

    /**
     * <pre>
     *  check permission (deprecated, use the check method)
     *  Deprecated: use directory.reader.v3.Check()
     * </pre>
     */
    @Deprecated
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request);

    /**
     * <pre>
     *  check relation (deprecated, use the check method)
     *  Deprecated: use directory.reader.v3.Check()
     * </pre>
     */
    @Deprecated
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request);

    /**
     * <pre>
     *  get object relationship graph
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request);
}
