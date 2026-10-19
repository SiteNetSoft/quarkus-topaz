package aserto.directory.model.v3;

import java.util.function.BiFunction;
import io.quarkus.grpc.MutinyClient;

@jakarta.annotation.Generated(value = "by Mutiny Grpc generator", comments = "Source: aserto/directory/model/v3/model.proto")
public class ModelClient implements Model, MutinyClient<MutinyModelGrpc.MutinyModelStub> {

    private final MutinyModelGrpc.MutinyModelStub stub;

    public ModelClient(String name, io.grpc.Channel channel, BiFunction<String, MutinyModelGrpc.MutinyModelStub, MutinyModelGrpc.MutinyModelStub> stubConfigurator) {
        this.stub = stubConfigurator.apply(name, MutinyModelGrpc.newMutinyStub(channel));
    }

    private ModelClient(MutinyModelGrpc.MutinyModelStub stub) {
        this.stub = stub;
    }

    public ModelClient newInstanceWithStub(MutinyModelGrpc.MutinyModelStub stub) {
        return new ModelClient(stub);
    }

    @Override
    public MutinyModelGrpc.MutinyModelStub getStub() {
        return stub;
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.DeleteManifestResponse> deleteManifest(aserto.directory.model.v3.ModelOuterClass.DeleteManifestRequest request) {
        return stub.deleteManifest(request);
    }

    @Override
    public io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.GetManifestResponse> getManifest(aserto.directory.model.v3.ModelOuterClass.GetManifestRequest request) {
        return stub.getManifest(request);
    }

    @Override
    public io.smallrye.mutiny.Uni<aserto.directory.model.v3.ModelOuterClass.SetManifestResponse> setManifest(io.smallrye.mutiny.Multi<aserto.directory.model.v3.ModelOuterClass.SetManifestRequest> request) {
        return stub.setManifest(request);
    }
}
