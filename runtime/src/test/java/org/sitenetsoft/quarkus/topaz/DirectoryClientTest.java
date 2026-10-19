package org.sitenetsoft.quarkus.topaz;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import aserto.directory.common.v3.Relation;
import aserto.directory.importer.v3.ImportCounter;
import aserto.directory.importer.v3.ImportRequest;
import aserto.directory.importer.v3.ImportResponse;
import aserto.directory.importer.v3.ImportStatus;
import aserto.directory.importer.v3.ImporterGrpc;
import aserto.directory.importer.v3.Opcode;
import aserto.directory.model.v3.Body;
import aserto.directory.model.v3.GetManifestRequest;
import aserto.directory.model.v3.GetManifestResponse;
import aserto.directory.model.v3.Metadata;
import aserto.directory.model.v3.ModelGrpc;
import aserto.directory.model.v3.SetManifestRequest;
import aserto.directory.model.v3.SetManifestResponse;
import aserto.directory.reader.v3.CheckRequest;
import aserto.directory.reader.v3.CheckResponse;
import aserto.directory.reader.v3.ChecksRequest;
import aserto.directory.reader.v3.ChecksResponse;
import aserto.directory.reader.v3.ReaderGrpc;
import aserto.directory.writer.v3.SetRelationRequest;
import aserto.directory.writer.v3.SetRelationResponse;
import aserto.directory.writer.v3.WriterGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Multi;

class DirectoryClientTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    /** Check results keyed by object id; unknown ids answer false. */
    private final Map<String, Boolean> checkResults = Map.of("doc-1", true, "doc-2", false);
    private final AtomicReference<CheckRequest> lastCheck = new AtomicReference<>();
    private final AtomicReference<String> receivedManifest = new AtomicReference<>();
    private final AtomicReference<Integer> manifestChunks = new AtomicReference<>();

    /** Set by any fake whose request stream fails; asserted empty after each test. */
    private static final AtomicReference<Throwable> serverError = new AtomicReference<>();

    private Server server;
    private ManagedChannel channel;
    private TopazClient client;

    @BeforeEach
    void start() throws Exception {
        String name = "dir-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new FakeReader())
                .addService(new FakeWriter())
                .addService(new FakeModel())
                .addService(new FakeImporter())
                .build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = TopazClient.builder().authorizerChannel(channel).directoryChannel(channel).build();
    }

    @AfterEach
    void stop() {
        Throwable err = serverError.getAndSet(null);
        client.close();
        client.close(); // idempotent
        channel.shutdownNow();
        server.shutdownNow();
        assertThat(err).as("fake server stream error").isNull();
    }

    @Test
    void checkMapsFields() {
        Boolean result = client.directory().check("doc", "doc-1", "owner", "user", "rick").await().atMost(WAIT);

        assertThat(result).isTrue();
        CheckRequest r = lastCheck.get();
        assertThat(r.getObjectType()).isEqualTo("doc");
        assertThat(r.getObjectId()).isEqualTo("doc-1");
        assertThat(r.getRelation()).isEqualTo("owner");
        assertThat(r.getSubjectType()).isEqualTo("user");
        assertThat(r.getSubjectId()).isEqualTo("rick");

        assertThat(client.directory().check("doc", "doc-2", "owner", "user", "rick").await().atMost(WAIT)).isFalse();

        List<Boolean> many = client.directory().checks(List.of(
                CheckRequest.newBuilder().setObjectId("doc-1").build(),
                CheckRequest.newBuilder().setObjectId("doc-2").build(),
                CheckRequest.newBuilder().setObjectId("doc-1").build())).await().atMost(WAIT);
        assertThat(many).containsExactly(true, false, true);
    }

    @Test
    void getManifestConcatenatesChunks() {
        assertThat(client.directory().getManifest().await().atMost(WAIT)).isEqualTo("model: 3");
        assertThat(client.blocking().directory().getManifest()).isEqualTo("model: 3");
    }

    @Test
    void setManifestSendsBody() {
        String yaml = "# yaml-language-server\nmodel:\n  version: 3\ntypes:\n  user: {}\n";
        client.directory().setManifest(yaml).await().atMost(WAIT);

        assertThat(receivedManifest.get()).isEqualTo(yaml);
        assertThat(manifestChunks.get()).isEqualTo(1);
    }

    @Test
    void setManifestChunksLargeManifests() {
        String yaml = "x".repeat(64 * 1024 * 2 + 10);
        client.directory().setManifest(yaml).await().atMost(WAIT);

        assertThat(receivedManifest.get()).isEqualTo(yaml);
        assertThat(manifestChunks.get()).isEqualTo(3);
    }

    @Test
    void importDataSummarises() {
        Multi<ImportRequest> requests = Multi.createFrom().items(
                ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET)
                        .setObject(aserto.directory.common.v3.Object.newBuilder().setType("user").setId("rick")).build(),
                ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET)
                        .setObject(aserto.directory.common.v3.Object.newBuilder().setType("doc").setId("doc-1")).build(),
                ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET)
                        .setRelation(Relation.newBuilder().setObjectType("doc").setObjectId("doc-1")
                                .setRelation("owner").setSubjectType("user").setSubjectId("rick"))
                        .build());

        ImportSummary summary = client.directory().importData(requests).await().atMost(WAIT);

        assertThat(summary).isEqualTo(new ImportSummary(2, 0, 1, 0, List.of()));
    }

    @Test
    void setRelationReturnsResult() {
        Relation r = Relation.newBuilder().setObjectType("doc").setObjectId("doc-1").setRelation("owner")
                .setSubjectType("user").setSubjectId("rick").build();
        Relation result = client.directory().setRelation(r).await().atMost(WAIT);
        assertThat(result.getEtag()).isEqualTo("e1");
        assertThat(result.getObjectId()).isEqualTo("doc-1");
    }

    @Test
    void summarisePrefersTypedCountersAndCollectsErrors() {
        ImportCounter legacyObjects = ImportCounter.newBuilder().setSet(99).build();
        ImportSummary summary = DirectoryClient.summarise(List.of(
                ImportResponse.newBuilder().setStatus(ImportStatus.newBuilder().setCode(3).setMsg("bad relation")).build(),
                ImportResponse.newBuilder().setStatus(ImportStatus.newBuilder().setCode(0).setMsg("ok")).build(),
                ImportResponse.newBuilder().setCounter(ImportCounter.newBuilder().setType("object").setSet(4).setDelete(1)).build(),
                ImportResponse.newBuilder().setCounter(ImportCounter.newBuilder().setType("relation").setSet(2).setDelete(5)).build(),
                ImportResponse.newBuilder().setObject(legacyObjects).build()));

        assertThat(summary).isEqualTo(new ImportSummary(4, 1, 2, 5, List.of("bad relation")));
    }

    private boolean resultFor(CheckRequest r) {
        return checkResults.getOrDefault(r.getObjectId(), false);
    }

    private final class FakeReader extends ReaderGrpc.ReaderImplBase {
        @Override
        public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
            lastCheck.set(request);
            responseObserver.onNext(CheckResponse.newBuilder().setCheck(resultFor(request)).build());
            responseObserver.onCompleted();
        }

        @Override
        public void checks(ChecksRequest request, StreamObserver<ChecksResponse> responseObserver) {
            ChecksResponse.Builder b = ChecksResponse.newBuilder();
            request.getChecksList().forEach(c -> b.addChecks(CheckResponse.newBuilder().setCheck(resultFor(c))));
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }
    }

    private static final class FakeWriter extends WriterGrpc.WriterImplBase {
        @Override
        public void setRelation(SetRelationRequest request, StreamObserver<SetRelationResponse> responseObserver) {
            responseObserver.onNext(SetRelationResponse.newBuilder()
                    .setResult(request.getRelation().toBuilder().setEtag("e1")).build());
            responseObserver.onCompleted();
        }
    }

    private final class FakeModel extends ModelGrpc.ModelImplBase {
        @Override
        public void getManifest(GetManifestRequest request, StreamObserver<GetManifestResponse> responseObserver) {
            responseObserver.onNext(GetManifestResponse.newBuilder()
                    .setMetadata(Metadata.newBuilder().setEtag("m1")).build());
            responseObserver.onNext(body("mod"));
            responseObserver.onNext(body("el: 3"));
            responseObserver.onCompleted();
        }

        private GetManifestResponse body(String s) {
            return GetManifestResponse.newBuilder()
                    .setBody(Body.newBuilder().setData(ByteString.copyFromUtf8(s))).build();
        }

        @Override
        public StreamObserver<SetManifestRequest> setManifest(StreamObserver<SetManifestResponse> responseObserver) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int[] chunks = { 0 };
            return new StreamObserver<>() {
                @Override
                public void onNext(SetManifestRequest value) {
                    chunks[0]++;
                    buf.writeBytes(value.getBody().getData().toByteArray());
                }

                @Override
                public void onError(Throwable t) {
                    serverError.set(t);
                }

                @Override
                public void onCompleted() {
                    receivedManifest.set(buf.toString(StandardCharsets.UTF_8));
                    manifestChunks.set(chunks[0]);
                    responseObserver.onNext(SetManifestResponse.getDefaultInstance());
                    responseObserver.onCompleted();
                }
            };
        }
    }

    private static final class FakeImporter extends ImporterGrpc.ImporterImplBase {
        @Override
        public StreamObserver<ImportRequest> import_(StreamObserver<ImportResponse> responseObserver) {
            long[] counts = { 0, 0 };
            return new StreamObserver<>() {
                @Override
                public void onNext(ImportRequest value) {
                    if (value.hasObject()) {
                        counts[0]++;
                    } else if (value.hasRelation()) {
                        counts[1]++;
                    }
                }

                @Override
                public void onError(Throwable t) {
                    serverError.set(t);
                }

                @Override
                public void onCompleted() {
                    responseObserver.onNext(ImportResponse.newBuilder()
                            .setObject(ImportCounter.newBuilder().setType("object").setRecv(counts[0]).setSet(counts[0]))
                            .setRelation(ImportCounter.newBuilder().setType("relation").setRecv(counts[1]).setSet(counts[1]))
                            .build());
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
