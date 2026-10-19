package org.sitenetsoft.quarkus.topaz;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;

import aserto.directory.common.v3.Relation;
import aserto.directory.exporter.v3.ExportRequest;
import aserto.directory.exporter.v3.ExportResponse;
import aserto.directory.exporter.v3.MutinyExporterGrpc;
import aserto.directory.importer.v3.ImportCounter;
import aserto.directory.importer.v3.ImportRequest;
import aserto.directory.importer.v3.ImportResponse;
import aserto.directory.importer.v3.MutinyImporterGrpc;
import aserto.directory.model.v3.Body;
import aserto.directory.model.v3.DeleteManifestRequest;
import aserto.directory.model.v3.GetManifestRequest;
import aserto.directory.model.v3.GetManifestResponse;
import aserto.directory.model.v3.MutinyModelGrpc;
import aserto.directory.model.v3.SetManifestRequest;
import aserto.directory.reader.v3.CheckRequest;
import aserto.directory.reader.v3.CheckResponse;
import aserto.directory.reader.v3.ChecksRequest;
import aserto.directory.reader.v3.GetObjectRequest;
import aserto.directory.reader.v3.GetObjectResponse;
import aserto.directory.reader.v3.GetRelationRequest;
import aserto.directory.reader.v3.GetRelationResponse;
import aserto.directory.reader.v3.MutinyReaderGrpc;
import aserto.directory.writer.v3.DeleteObjectRequest;
import aserto.directory.writer.v3.DeleteRelationRequest;
import aserto.directory.writer.v3.MutinyWriterGrpc;
import aserto.directory.writer.v3.SetObjectRequest;
import aserto.directory.writer.v3.SetObjectResponse;
import aserto.directory.writer.v3.SetRelationRequest;
import aserto.directory.writer.v3.SetRelationResponse;
import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Reactive client of the Topaz directory services (reader, writer, model, importer, exporter).
 */
public final class DirectoryClient {

    /** Maximum size of one manifest {@code Body} chunk sent to the model service. */
    static final int MANIFEST_CHUNK_SIZE = 64 * 1024;

    private final MutinyReaderGrpc.MutinyReaderStub reader;
    private final MutinyWriterGrpc.MutinyWriterStub writer;
    private final MutinyModelGrpc.MutinyModelStub model;
    private final MutinyImporterGrpc.MutinyImporterStub importer;
    private final MutinyExporterGrpc.MutinyExporterStub exporter;
    private final Optional<Duration> deadline;

    DirectoryClient(Channel channel, Optional<Duration> deadline) {
        this.reader = MutinyReaderGrpc.newMutinyStub(channel);
        this.writer = MutinyWriterGrpc.newMutinyStub(channel);
        this.model = MutinyModelGrpc.newMutinyStub(channel);
        this.importer = MutinyImporterGrpc.newMutinyStub(channel);
        this.exporter = MutinyExporterGrpc.newMutinyStub(channel);
        this.deadline = deadline;
    }

    Optional<Duration> deadline() {
        return deadline;
    }

    private <S extends AbstractStub<S>> S withDeadline(S stub) {
        return deadline.map(d -> stub.withDeadlineAfter(d.toMillis(), TimeUnit.MILLISECONDS)).orElse(stub);
    }

    /** The raw reader stub, with the configured deadline (if any) starting now; likewise for the others. */
    public MutinyReaderGrpc.MutinyReaderStub reader() {
        return withDeadline(reader);
    }

    public MutinyWriterGrpc.MutinyWriterStub writer() {
        return withDeadline(writer);
    }

    public MutinyModelGrpc.MutinyModelStub model() {
        return withDeadline(model);
    }

    public MutinyImporterGrpc.MutinyImporterStub importer() {
        return withDeadline(importer);
    }

    public MutinyExporterGrpc.MutinyExporterStub exporter() {
        return withDeadline(exporter);
    }

    public Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return reader().check(CheckRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build()).map(CheckResponse::getCheck);
    }

    /** Runs several checks in one call; results are in request order. */
    public Uni<List<Boolean>> checks(List<CheckRequest> checks) {
        return reader().checks(ChecksRequest.newBuilder().addAllChecks(checks).build())
                .map(r -> r.getChecksList().stream().map(CheckResponse::getCheck).toList());
    }

    public Uni<aserto.directory.common.v3.Object> getObject(String type, String id) {
        return reader().getObject(GetObjectRequest.newBuilder().setObjectType(type).setObjectId(id).build())
                .map(GetObjectResponse::getResult);
    }

    public Uni<aserto.directory.common.v3.Object> setObject(aserto.directory.common.v3.Object o) {
        return writer().setObject(SetObjectRequest.newBuilder().setObject(o).build())
                .map(SetObjectResponse::getResult);
    }

    public Uni<Void> deleteObject(String type, String id, boolean withRelations) {
        return writer().deleteObject(DeleteObjectRequest.newBuilder()
                .setObjectType(type).setObjectId(id).setWithRelations(withRelations).build())
                .replaceWithVoid();
    }

    public Uni<Relation> getRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return reader().getRelation(GetRelationRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build()).map(GetRelationResponse::getResult);
    }

    public Uni<Relation> setRelation(Relation r) {
        return writer().setRelation(SetRelationRequest.newBuilder().setRelation(r).build())
                .map(SetRelationResponse::getResult);
    }

    public Uni<Void> deleteRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return writer().deleteRelation(DeleteRelationRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build()).replaceWithVoid();
    }

    /** The manifest YAML: the UTF-8 decoding of all {@code Body} chunks; metadata/model messages are ignored. */
    public Uni<String> getManifest() {
        return model().getManifest(GetManifestRequest.getDefaultInstance())
                .filter(GetManifestResponse::hasBody)
                .map(r -> r.getBody().getData())
                .collect().asList()
                .map(chunks -> ByteString.copyFrom(chunks).toString(StandardCharsets.UTF_8));
    }

    /** Uploads the manifest YAML in {@code Body} chunks of at most 64 KiB (one chunk for small manifests). */
    public Uni<Void> setManifest(String yaml) {
        byte[] bytes = yaml.getBytes(StandardCharsets.UTF_8);
        List<SetManifestRequest> requests = new ArrayList<>();
        int offset = 0;
        do {
            int len = Math.min(MANIFEST_CHUNK_SIZE, bytes.length - offset);
            requests.add(SetManifestRequest.newBuilder()
                    .setBody(Body.newBuilder().setData(ByteString.copyFrom(bytes, offset, len)))
                    .build());
            offset += len;
        } while (offset < bytes.length);
        return model().setManifest(Multi.createFrom().iterable(requests)).replaceWithVoid();
    }

    public Uni<Void> deleteManifest() {
        return model().deleteManifest(DeleteManifestRequest.getDefaultInstance()).replaceWithVoid();
    }

    /**
     * Streams {@code requests} to the importer and sums the counters it reports. Typed {@code counter} messages
     * (oneof {@code msg}) are preferred; the deprecated {@code object}/{@code relation} fields are used only if
     * no typed counter was received, so a server sending both is not double counted.
     */
    public Uni<ImportSummary> importData(Multi<ImportRequest> requests) {
        return importer().import_(requests).collect().asList().map(DirectoryClient::summarise);
    }

    static ImportSummary summarise(List<ImportResponse> responses) {
        long objectsSet = 0, objectsDeleted = 0, relationsSet = 0, relationsDeleted = 0;
        List<String> errors = new ArrayList<>();
        boolean typedCounters = responses.stream()
                .anyMatch(r -> r.getMsgCase() == ImportResponse.MsgCase.COUNTER);
        for (ImportResponse r : responses) {
            if (r.getMsgCase() == ImportResponse.MsgCase.STATUS && r.getStatus().getCode() != 0) {
                String msg = r.getStatus().getMsg();
                errors.add(msg.isEmpty() ? "import error code " + r.getStatus().getCode() : msg);
            }
            if (typedCounters) {
                if (r.getMsgCase() == ImportResponse.MsgCase.COUNTER) {
                    ImportCounter c = r.getCounter();
                    if ("object".equals(c.getType())) {
                        objectsSet += c.getSet();
                        objectsDeleted += c.getDelete();
                    } else if ("relation".equals(c.getType())) {
                        relationsSet += c.getSet();
                        relationsDeleted += c.getDelete();
                    }
                }
            } else {
                if (r.hasObject()) {
                    objectsSet += r.getObject().getSet();
                    objectsDeleted += r.getObject().getDelete();
                }
                if (r.hasRelation()) {
                    relationsSet += r.getRelation().getSet();
                    relationsDeleted += r.getRelation().getDelete();
                }
            }
        }
        return new ImportSummary(objectsSet, objectsDeleted, relationsSet, relationsDeleted, errors);
    }

    public Multi<ExportResponse> export(ExportRequest r) {
        return exporter().export(r);
    }
}
