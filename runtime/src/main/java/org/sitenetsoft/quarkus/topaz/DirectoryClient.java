package org.sitenetsoft.quarkus.topaz;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
import io.grpc.Status;
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

    /**
     * Defers stub acquisition to subscription time, so each subscription (including retries and late
     * subscriptions) gets a deadline that starts when the call is actually made. The deadline is also enforced
     * here with a Mutiny timeout: the gRPC deadline alone does not end a call on a connection that never
     * completes its HTTP/2 handshake.
     */
    private <S, T> Uni<T> uni(java.util.function.Supplier<S> stub, Function<S, Uni<T>> f) {
        Uni<T> call = Uni.createFrom().deferred(() -> f.apply(stub.get()));
        return deadline.map(d -> call.ifNoItem().after(d).failWith(() -> deadlineExceeded(d))).orElse(call);
    }

    /** Same as {@link #uni}; for a stream the guard is an inactivity timeout between items, not a first-item timeout. */
    private <S, T> Multi<T> multi(java.util.function.Supplier<S> stub, Function<S, Multi<T>> f) {
        Multi<T> call = Multi.createFrom().deferred(() -> f.apply(stub.get()));
        return deadline.map(d -> call.ifNoItem().after(d).failWith(() -> deadlineExceeded(d))).orElse(call);
    }

    static Throwable deadlineExceeded(Duration deadline) {
        return Status.DEADLINE_EXCEEDED
                .withDescription("Topaz call exceeded the configured deadline of " + deadline).asRuntimeException();
    }

    /** The raw reader stub, with the configured deadline (if any) starting now; likewise for the others. */
    public MutinyReaderGrpc.MutinyReaderStub reader() {
        return withDeadline(reader);
    }

    /** The raw writer stub, with the configured deadline (if any) starting now. */
    public MutinyWriterGrpc.MutinyWriterStub writer() {
        return withDeadline(writer);
    }

    /** The raw model stub, with the configured deadline (if any) starting now. */
    public MutinyModelGrpc.MutinyModelStub model() {
        return withDeadline(model);
    }

    /** The raw importer stub, with the configured deadline (if any) starting now. */
    public MutinyImporterGrpc.MutinyImporterStub importer() {
        return withDeadline(importer);
    }

    /** The raw exporter stub, with the configured deadline (if any) starting now. */
    public MutinyExporterGrpc.MutinyExporterStub exporter() {
        return withDeadline(exporter);
    }

    /**
     * Calls the directory reader {@code Check} RPC; emits the check result, failing with a {@link io.grpc.StatusRuntimeException} on any error, never a default {@code true}.
     */
    public Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return uni(this::reader, s -> s.check(CheckRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build())).map(CheckResponse::getCheck);
    }

    /** Runs several checks in one call; results are in request order. */
    public Uni<List<Boolean>> checks(List<CheckRequest> checks) {
        return uni(this::reader, s -> s.checks(ChecksRequest.newBuilder().addAllChecks(checks).build()))
                .map(r -> r.getChecksList().stream().map(CheckResponse::getCheck).toList());
    }

    /** Calls the directory reader {@code GetObject} RPC. */
    public Uni<aserto.directory.common.v3.Object> getObject(String type, String id) {
        return uni(this::reader, s -> s.getObject(GetObjectRequest.newBuilder().setObjectType(type).setObjectId(id).build()))
                .map(GetObjectResponse::getResult);
    }

    /** Calls the directory writer {@code SetObject} RPC and emits the stored object. */
    public Uni<aserto.directory.common.v3.Object> setObject(aserto.directory.common.v3.Object o) {
        return uni(this::writer, s -> s.setObject(SetObjectRequest.newBuilder().setObject(o).build()))
                .map(SetObjectResponse::getResult);
    }

    /** Calls the directory writer {@code DeleteObject} RPC, optionally deleting the object's relations too. */
    public Uni<Void> deleteObject(String type, String id, boolean withRelations) {
        return uni(this::writer, s -> s.deleteObject(DeleteObjectRequest.newBuilder()
                .setObjectType(type).setObjectId(id).setWithRelations(withRelations).build()))
                .replaceWithVoid();
    }

    /** Calls the directory reader {@code GetRelation} RPC. */
    public Uni<Relation> getRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return uni(this::reader, s -> s.getRelation(GetRelationRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build())).map(GetRelationResponse::getResult);
    }

    /** Calls the directory writer {@code SetRelation} RPC and emits the stored relation. */
    public Uni<Relation> setRelation(Relation r) {
        return uni(this::writer, s -> s.setRelation(SetRelationRequest.newBuilder().setRelation(r).build()))
                .map(SetRelationResponse::getResult);
    }

    /** Calls the directory writer {@code DeleteRelation} RPC. */
    public Uni<Void> deleteRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return uni(this::writer, s -> s.deleteRelation(DeleteRelationRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build())).replaceWithVoid();
    }

    /** The manifest YAML: the UTF-8 decoding of all {@code Body} chunks; metadata/model messages are ignored. */
    public Uni<String> getManifest() {
        return multi(this::model, s -> s.getManifest(GetManifestRequest.getDefaultInstance()))
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
        return uni(this::model, s -> s.setManifest(Multi.createFrom().iterable(requests))).replaceWithVoid();
    }

    /** Calls the directory model {@code DeleteManifest} RPC. */
    public Uni<Void> deleteManifest() {
        return uni(this::model, s -> s.deleteManifest(DeleteManifestRequest.getDefaultInstance())).replaceWithVoid();
    }

    /**
     * Streams {@code requests} to the importer and sums the counters it reports. Typed {@code counter} messages
     * (oneof {@code msg}) are preferred; the deprecated {@code object}/{@code relation} fields are used only if
     * no typed counter was received, so a server sending both is not double counted.
     */
    public Uni<ImportSummary> importData(Multi<ImportRequest> requests) {
        return multi(this::importer, s -> s.import_(requests)).collect().asList().map(DirectoryClient::summarise);
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

    /** Calls the directory exporter {@code Export} RPC and streams its responses. */
    public Multi<ExportResponse> export(ExportRequest r) {
        return multi(this::exporter, s -> s.export(r));
    }
}
