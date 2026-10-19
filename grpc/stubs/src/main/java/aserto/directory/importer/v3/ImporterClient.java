package aserto.directory.importer.v3;

import java.util.function.BiFunction;
import io.quarkus.grpc.MutinyClient;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/importer/v3/importer.proto")
public class ImporterClient implements Importer, MutinyClient<MutinyImporterGrpc.MutinyImporterStub> {

    private final MutinyImporterGrpc.MutinyImporterStub stub;

    public ImporterClient(String name, io.grpc.Channel channel, BiFunction<String, MutinyImporterGrpc.MutinyImporterStub, MutinyImporterGrpc.MutinyImporterStub> stubConfigurator) {
        this.stub = stubConfigurator.apply(name, MutinyImporterGrpc.newMutinyStub(channel));
    }

    private ImporterClient(MutinyImporterGrpc.MutinyImporterStub stub) {
        this.stub = stub;
    }

    public ImporterClient newInstanceWithStub(MutinyImporterGrpc.MutinyImporterStub stub) {
        return new ImporterClient(stub);
    }

    @Override
    public MutinyImporterGrpc.MutinyImporterStub getStub() {
        return stub;
    }

    @Override
    public io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImportResponse> import_(io.smallrye.mutiny.Multi<aserto.directory.importer.v3.ImportRequest> request) {
        return stub.import_(request);
    }
}
