package aserto.directory.writer.v3;

import io.quarkus.grpc.MutinyService;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/writer/v3/writer.proto")
public interface Writer extends MutinyService {

    /**
     * <pre>
     *  set object instance
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.SetObjectResponse> setObject(aserto.directory.writer.v3.WriterOuterClass.SetObjectRequest request);

    /**
     * <pre>
     *  delete object instance
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.DeleteObjectResponse> deleteObject(aserto.directory.writer.v3.WriterOuterClass.DeleteObjectRequest request);

    /**
     * <pre>
     *  set relation instance
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.SetRelationResponse> setRelation(aserto.directory.writer.v3.WriterOuterClass.SetRelationRequest request);

    /**
     * <pre>
     *  delete relation instance
     * </pre>
     */
    io.smallrye.mutiny.Uni<aserto.directory.writer.v3.WriterOuterClass.DeleteRelationResponse> deleteRelation(aserto.directory.writer.v3.WriterOuterClass.DeleteRelationRequest request);
}
