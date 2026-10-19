package aserto.directory.reader.v3;

import java.util.function.BiFunction;
import io.quarkus.grpc.MutinyClient;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/reader/v3/reader.proto")
public class ReaderClient implements Reader, MutinyClient<MutinyReaderGrpc.MutinyReaderStub> {

    private final MutinyReaderGrpc.MutinyReaderStub stub;

    public ReaderClient(String name, io.grpc.Channel channel, BiFunction<String, MutinyReaderGrpc.MutinyReaderStub, MutinyReaderGrpc.MutinyReaderStub> stubConfigurator) {
        this.stub = stubConfigurator.apply(name, MutinyReaderGrpc.newMutinyStub(channel));
    }

    private ReaderClient(MutinyReaderGrpc.MutinyReaderStub stub) {
        this.stub = stub;
    }

    public ReaderClient newInstanceWithStub(MutinyReaderGrpc.MutinyReaderStub stub) {
        return new ReaderClient(stub);
    }

    @Override
    public MutinyReaderGrpc.MutinyReaderStub getStub() {
        return stub;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectResponse> getObject(aserto.directory.reader.v3.ReaderOuterClass.GetObjectRequest request) {
        return stub.getObject(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.ReaderOuterClass.GetObjectManyRequest request) {
        return stub.getObjectMany(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetObjectsResponse> getObjects(aserto.directory.reader.v3.ReaderOuterClass.GetObjectsRequest request) {
        return stub.getObjects(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetRelationResponse> getRelation(aserto.directory.reader.v3.ReaderOuterClass.GetRelationRequest request) {
        return stub.getRelation(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetRelationsResponse> getRelations(aserto.directory.reader.v3.ReaderOuterClass.GetRelationsRequest request) {
        return stub.getRelations(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckResponse> check(aserto.directory.reader.v3.ReaderOuterClass.CheckRequest request) {
        return stub.check(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.ChecksResponse> checks(aserto.directory.reader.v3.ReaderOuterClass.ChecksRequest request) {
        return stub.checks(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.ReaderOuterClass.CheckPermissionRequest request) {
        return stub.checkPermission(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.ReaderOuterClass.CheckRelationRequest request) {
        return stub.checkRelation(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ReaderOuterClass.GetGraphResponse> getGraph(aserto.directory.reader.v3.ReaderOuterClass.GetGraphRequest request) {
        return stub.getGraph(request);
    }
}
