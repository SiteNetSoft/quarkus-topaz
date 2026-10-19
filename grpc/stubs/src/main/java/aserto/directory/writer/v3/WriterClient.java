package aserto.directory.writer.v3;

import java.util.function.BiFunction;
import io.quarkus.grpc.MutinyClient;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/writer/v3/writer.proto")
public class WriterClient implements Writer, MutinyClient<MutinyWriterGrpc.MutinyWriterStub> {

    private final MutinyWriterGrpc.MutinyWriterStub stub;

    public WriterClient(String name, io.grpc.Channel channel, BiFunction<String, MutinyWriterGrpc.MutinyWriterStub, MutinyWriterGrpc.MutinyWriterStub> stubConfigurator) {
        this.stub = stubConfigurator.apply(name, MutinyWriterGrpc.newMutinyStub(channel));
    }

    private WriterClient(MutinyWriterGrpc.MutinyWriterStub stub) {
        this.stub = stub;
    }

    public WriterClient newInstanceWithStub(MutinyWriterGrpc.MutinyWriterStub stub) {
        return new WriterClient(stub);
    }

    @Override
    public MutinyWriterGrpc.MutinyWriterStub getStub() {
        return stub;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetObjectResponse> setObject(aserto.directory.writer.v3.SetObjectRequest request) {
        return stub.setObject(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteObjectResponse> deleteObject(aserto.directory.writer.v3.DeleteObjectRequest request) {
        return stub.deleteObject(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.SetRelationResponse> setRelation(aserto.directory.writer.v3.SetRelationRequest request) {
        return stub.setRelation(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.writer.v3.DeleteRelationResponse> deleteRelation(aserto.directory.writer.v3.DeleteRelationRequest request) {
        return stub.deleteRelation(request);
    }
}
