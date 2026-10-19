package org.sitenetsoft.quarkus.topaz.deployment.spike;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import com.google.protobuf.ByteString;

import aserto.authorizer.v2.AuthorizerGrpc;
import aserto.authorizer.v2.InfoRequest;
import aserto.directory.common.v3.Object;
import aserto.directory.common.v3.Relation;
import aserto.directory.importer.v3.ImportRequest;
import aserto.directory.importer.v3.MutinyImporterGrpc;
import aserto.directory.importer.v3.Opcode;
import aserto.directory.model.v3.Body;
import aserto.directory.model.v3.GetManifestRequest;
import aserto.directory.model.v3.GetManifestResponse;
import aserto.directory.model.v3.MutinyModelGrpc;
import aserto.directory.model.v3.SetManifestRequest;
import aserto.directory.reader.v3.CheckRequest;
import aserto.directory.reader.v3.MutinyReaderGrpc;
import io.grpc.Channel;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;

/** Throwaway spike, replaced in Task 8. */
class TopazPlaintextSpikeTest {

    static final String MANIFEST = """
            model:
              version: 3
            types:
              user: {}
              todo:
                relations:
                  owner: user
                permissions:
                  can_delete: owner
            """;

    static GenericContainer<?> topaz;
    static Vertx vertx;
    static Channel authCh;
    static Channel dirCh;

    @BeforeAll
    static void start() {
        topaz = new GenericContainer<>("ghcr.io/aserto-dev/topaz:0.33.22")
                .withEnv("TOPAZ_DB_DIR", "/db")
                .withEnv("TOPAZ_CERTS_DIR", "/certs")
                .withEnv("TOPAZ_DECISIONS_DIR", "/decisions")
                .withCopyFileToContainer(MountableFile.forClasspathResource("spike/config.yaml"),
                        "/config/config.yaml")
                .withCopyToContainer(org.testcontainers.images.builder.Transferable.of("package spike\n"),
                        "/policies/spike.rego")
                .withExposedPorts(8282, 9292, 9494)
                .waitingFor(Wait.forListeningPorts(8282, 9292))
                .withStartupTimeout(Duration.ofSeconds(90))
                .withLogConsumer(f -> System.out.print("[topaz] " + f.getUtf8String()));
        topaz.start();
        vertx = Vertx.vertx();
        GrpcClient client = GrpcClient.client(vertx);
        authCh = new GrpcClientChannel(client,
                SocketAddress.inetSocketAddress(topaz.getMappedPort(8282), topaz.getHost()));
        dirCh = new GrpcClientChannel(client,
                SocketAddress.inetSocketAddress(topaz.getMappedPort(9292), topaz.getHost()));
    }

    @AfterAll
    static void stop() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
        if (topaz != null) {
            topaz.stop();
        }
    }

    @Test
    void plaintextUnaryAndStreaming() {
        assertThat(AuthorizerGrpc.newBlockingStub(authCh).info(InfoRequest.getDefaultInstance()).getVersion())
                .isNotEmpty();

        MutinyModelGrpc.MutinyModelStub model = MutinyModelGrpc.newMutinyStub(dirCh);
        model.setManifest(Multi.createFrom().item(SetManifestRequest.newBuilder()
                .setBody(Body.newBuilder().setData(ByteString.copyFromUtf8(MANIFEST))).build()))
                .await().atMost(Duration.ofSeconds(30));
        String manifest = "";
        for (int i = 0; i < 20 && !manifest.contains("can_delete"); i++) {
            // Topaz serves reads from a read-only DB handle that can briefly lag a write
            List<GetManifestResponse> got = model.getManifest(GetManifestRequest.getDefaultInstance())
                    .collect().asList().await().atMost(Duration.ofSeconds(30));
            System.out.println("GETMANIFEST attempt " + i + " messages=" + got.size());
            manifest = got.stream().filter(GetManifestResponse::hasBody)
                    .map(r -> r.getBody().getData().toStringUtf8()).reduce("", String::concat);
            if (!manifest.contains("can_delete")) {
                try { Thread.sleep(250); } catch (InterruptedException e) { throw new RuntimeException(e); }
            }
        }
        assertThat(manifest).isEqualTo(MANIFEST);

        MutinyImporterGrpc.MutinyImporterStub imp = MutinyImporterGrpc.newMutinyStub(dirCh);
        imp.import_(Multi.createFrom().items(
                ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET)
                        .setObject(Object.newBuilder().setType("user").setId("rick")).build(),
                ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET)
                        .setObject(Object.newBuilder().setType("todo").setId("todo-1")).build(),
                ImportRequest.newBuilder().setOpCode(Opcode.OPCODE_SET)
                        .setRelation(Relation.newBuilder().setObjectType("todo").setObjectId("todo-1")
                                .setRelation("owner").setSubjectType("user").setSubjectId("rick"))
                        .build()))
                .collect().asList().await().atMost(Duration.ofSeconds(30));

        boolean ok = MutinyReaderGrpc.newMutinyStub(dirCh).check(CheckRequest.newBuilder()
                .setObjectType("todo").setObjectId("todo-1").setRelation("owner")
                .setSubjectType("user").setSubjectId("rick").build())
                .await().atMost(Duration.ofSeconds(30)).getCheck();
        assertThat(ok).isTrue();
    }
}
