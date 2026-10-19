package aserto.directory.exporter.v3;

import java.util.function.BiFunction;
import io.quarkus.grpc.MutinyClient;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/exporter/v3/exporter.proto")
public class ExporterClient implements Exporter, MutinyClient<MutinyExporterGrpc.MutinyExporterStub> {

    private final MutinyExporterGrpc.MutinyExporterStub stub;

    public ExporterClient(String name, io.grpc.Channel channel, BiFunction<String, MutinyExporterGrpc.MutinyExporterStub, MutinyExporterGrpc.MutinyExporterStub> stubConfigurator) {
        this.stub = stubConfigurator.apply(name, MutinyExporterGrpc.newMutinyStub(channel));
    }

    private ExporterClient(MutinyExporterGrpc.MutinyExporterStub stub) {
        this.stub = stub;
    }

    public ExporterClient newInstanceWithStub(MutinyExporterGrpc.MutinyExporterStub stub) {
        return new ExporterClient(stub);
    }

    @Override
    public MutinyExporterGrpc.MutinyExporterStub getStub() {
        return stub;
    }

    @Override
    public io.smallrye.mutiny.Multi<aserto.directory.exporter.v3.ExportResponse> export(aserto.directory.exporter.v3.ExportRequest request) {
        return stub.export(request);
    }
}
