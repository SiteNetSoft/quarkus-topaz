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
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectResponse> getObject(aserto.directory.reader.v3.GetObjectRequest request) {
        return stub.getObject(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectManyResponse> getObjectMany(aserto.directory.reader.v3.GetObjectManyRequest request) {
        return stub.getObjectMany(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetObjectsResponse> getObjects(aserto.directory.reader.v3.GetObjectsRequest request) {
        return stub.getObjects(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationResponse> getRelation(aserto.directory.reader.v3.GetRelationRequest request) {
        return stub.getRelation(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetRelationsResponse> getRelations(aserto.directory.reader.v3.GetRelationsRequest request) {
        return stub.getRelations(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckResponse> check(aserto.directory.reader.v3.CheckRequest request) {
        return stub.check(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.ChecksResponse> checks(aserto.directory.reader.v3.ChecksRequest request) {
        return stub.checks(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckPermissionResponse> checkPermission(aserto.directory.reader.v3.CheckPermissionRequest request) {
        return stub.checkPermission(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.CheckRelationResponse> checkRelation(aserto.directory.reader.v3.CheckRelationRequest request) {
        return stub.checkRelation(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.reader.v3.GetGraphResponse> getGraph(aserto.directory.reader.v3.GetGraphRequest request) {
        return stub.getGraph(request);
    }
}
