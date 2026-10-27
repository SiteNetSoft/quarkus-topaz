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
 * Reactive client of the Topaz directory services: reader, writer, model, importer and exporter.
 * Every call applies the configured deadline, if any.
 * A failed call fails the returned {@link Uni} or {@link Multi} with a {@link io.grpc.StatusRuntimeException}.
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

    /**
     * Create a client.
     *
     * @param channel the channel to the directory services
     * @param deadline the deadline of each call, if any
     */
    DirectoryClient(Channel channel, Optional<Duration> deadline) {
        this.reader = MutinyReaderGrpc.newMutinyStub(channel);
        this.writer = MutinyWriterGrpc.newMutinyStub(channel);
        this.model = MutinyModelGrpc.newMutinyStub(channel);
        this.importer = MutinyImporterGrpc.newMutinyStub(channel);
        this.exporter = MutinyExporterGrpc.newMutinyStub(channel);
        this.deadline = deadline;
    }

    /**
     * Get the configured deadline.
     *
     * @return the deadline, if any
     */
    Optional<Duration> deadline() {
        return deadline;
    }

    private <S extends AbstractStub<S>> S withDeadline(S stub) {
        return deadline.map(d -> stub.withDeadlineAfter(d.toMillis(), TimeUnit.MILLISECONDS)).orElse(stub);
    }

    // Defer stub acquisition to subscription time.
    // Each subscription, including a retry or a late one, then gets a deadline that starts when the call is made.
    // A Mutiny timeout enforces the deadline too, because the gRPC deadline alone does not end a call
    // on a connection that never completes its HTTP/2 handshake.
    private <S, T> Uni<T> uni(java.util.function.Supplier<S> stub, Function<S, Uni<T>> f) {
        Uni<T> call = Uni.createFrom().deferred(() -> f.apply(stub.get()));
        return deadline.map(d -> call.ifNoItem().after(d).failWith(() -> deadlineExceeded(d))).orElse(call);
    }

    // Same as uni(). For a stream the guard is an inactivity timeout between items, not a first-item timeout.
    private <S, T> Multi<T> multi(java.util.function.Supplier<S> stub, Function<S, Multi<T>> f) {
        Multi<T> call = Multi.createFrom().deferred(() -> f.apply(stub.get()));
        return deadline.map(d -> call.ifNoItem().after(d).failWith(() -> deadlineExceeded(d))).orElse(call);
    }

    /**
     * Create the failure for an exceeded deadline.
     *
     * @param deadline the deadline that passed
     * @return a {@code DEADLINE_EXCEEDED} {@link io.grpc.StatusRuntimeException}
     */
    static Throwable deadlineExceeded(Duration deadline) {
        return Status.DEADLINE_EXCEEDED
                .withDescription("Topaz call exceeded the configured deadline of " + deadline).asRuntimeException();
    }

    /**
     * Get the raw reader stub, with the configured deadline, if any, starting now.
     *
     * @return the raw reader stub
     */
    public MutinyReaderGrpc.MutinyReaderStub reader() {
        return withDeadline(reader);
    }

    /**
     * Get the raw writer stub, with the configured deadline, if any, starting now.
     *
     * @return the raw writer stub
     */
    public MutinyWriterGrpc.MutinyWriterStub writer() {
        return withDeadline(writer);
    }

    /**
     * Get the raw model stub, with the configured deadline, if any, starting now.
     *
     * @return the raw model stub
     */
    public MutinyModelGrpc.MutinyModelStub model() {
        return withDeadline(model);
    }

    /**
     * Get the raw importer stub, with the configured deadline, if any, starting now.
     *
     * @return the raw importer stub
     */
    public MutinyImporterGrpc.MutinyImporterStub importer() {
        return withDeadline(importer);
    }

    /**
     * Get the raw exporter stub, with the configured deadline, if any, starting now.
     *
     * @return the raw exporter stub
     */
    public MutinyExporterGrpc.MutinyExporterStub exporter() {
        return withDeadline(exporter);
    }

    /**
     * Call the directory reader {@code Check} RPC.
     * The call fails closed.
     * An error fails with a {@link io.grpc.StatusRuntimeException} and never yields a default {@code true}.
     *
     * @param objectType the object type
     * @param objectId the object id
     * @param relation the relation name
     * @param subjectType the subject type
     * @param subjectId the subject id
     * @return the check result
     */
    public Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return uni(this::reader, s -> s.check(CheckRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build())).map(CheckResponse::getCheck);
    }

    /**
     * Run several checks in one call.
     *
     * @param checks the checks to run
     * @return the results, in request order
     */
    public Uni<List<Boolean>> checks(List<CheckRequest> checks) {
        return uni(this::reader, s -> s.checks(ChecksRequest.newBuilder().addAllChecks(checks).build()))
                .map(r -> r.getChecksList().stream().map(CheckResponse::getCheck).toList());
    }

    /**
     * Call the directory reader {@code GetObject} RPC.
     *
     * @param type the object type
     * @param id the object id
     * @return the object
     */
    public Uni<aserto.directory.common.v3.Object> getObject(String type, String id) {
        return uni(this::reader, s -> s.getObject(GetObjectRequest.newBuilder().setObjectType(type).setObjectId(id).build()))
                .map(GetObjectResponse::getResult);
    }

    /**
     * Call the directory writer {@code SetObject} RPC.
     *
     * @param o the object to store
     * @return the stored object
     */
    public Uni<aserto.directory.common.v3.Object> setObject(aserto.directory.common.v3.Object o) {
        return uni(this::writer, s -> s.setObject(SetObjectRequest.newBuilder().setObject(o).build()))
                .map(SetObjectResponse::getResult);
    }

    /**
     * Call the directory writer {@code DeleteObject} RPC.
     * It can delete the relations of the object too.
     *
     * @param type the object type
     * @param id the object id
     * @param withRelations whether to delete the relations of the object too
     * @return a {@link Uni} that completes when the object is deleted
     */
    public Uni<Void> deleteObject(String type, String id, boolean withRelations) {
        return uni(this::writer, s -> s.deleteObject(DeleteObjectRequest.newBuilder()
                .setObjectType(type).setObjectId(id).setWithRelations(withRelations).build()))
                .replaceWithVoid();
    }

    /**
     * Call the directory reader {@code GetRelation} RPC.
     *
     * @param objectType the object type
     * @param objectId the object id
     * @param relation the relation name
     * @param subjectType the subject type
     * @param subjectId the subject id
     * @return the relation
     */
    public Uni<Relation> getRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return uni(this::reader, s -> s.getRelation(GetRelationRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build())).map(GetRelationResponse::getResult);
    }

    /**
     * Call the directory writer {@code SetRelation} RPC.
     *
     * @param r the relation to store
     * @return the stored relation
     */
    public Uni<Relation> setRelation(Relation r) {
        return uni(this::writer, s -> s.setRelation(SetRelationRequest.newBuilder().setRelation(r).build()))
                .map(SetRelationResponse::getResult);
    }

    /**
     * Call the directory writer {@code DeleteRelation} RPC.
     *
     * @param objectType the object type
     * @param objectId the object id
     * @param relation the relation name
     * @param subjectType the subject type
     * @param subjectId the subject id
     * @return a {@link Uni} that completes when the relation is deleted
     */
    public Uni<Void> deleteRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return uni(this::writer, s -> s.deleteRelation(DeleteRelationRequest.newBuilder()
                .setObjectType(objectType).setObjectId(objectId).setRelation(relation)
                .setSubjectType(subjectType).setSubjectId(subjectId)
                .build())).replaceWithVoid();
    }

    /**
     * Get the manifest YAML.
     * The YAML is the UTF-8 decoding of all {@code Body} chunks.
     * The call ignores metadata and model messages.
     *
     * @return the manifest YAML
     */
    public Uni<String> getManifest() {
        return multi(this::model, s -> s.getManifest(GetManifestRequest.getDefaultInstance()))
                .filter(GetManifestResponse::hasBody)
                .map(r -> r.getBody().getData())
                .collect().asList()
                .map(chunks -> ByteString.copyFrom(chunks).toString(StandardCharsets.UTF_8));
    }

    /**
     * Upload the manifest YAML in {@code Body} chunks of at most 64 KiB.
     * A small manifest travels in one chunk.
     *
     * @param yaml the manifest YAML
     * @return a {@link Uni} that completes when the upload ends
     */
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

    /**
     * Call the directory model {@code DeleteManifest} RPC.
     *
     * @return a {@link Uni} that completes when the manifest is deleted
     */
    public Uni<Void> deleteManifest() {
        return uni(this::model, s -> s.deleteManifest(DeleteManifestRequest.getDefaultInstance())).replaceWithVoid();
    }

    /**
     * Stream {@code requests} to the importer and sum the counters it reports.
     * The sum prefers the typed {@code counter} messages, which are the {@code msg} oneof.
     * It uses the deprecated {@code object} and {@code relation} fields only if no typed counter arrived.
     * A server that sends both is therefore not double counted.
     *
     * @param requests the import requests
     * @return the import totals
     */
    public Uni<ImportSummary> importData(Multi<ImportRequest> requests) {
        return multi(this::importer, s -> s.import_(requests)).collect().asList().map(DirectoryClient::summarise);
    }

    /**
     * Sum the counters of the importer responses.
     *
     * @param responses the importer responses
     * @return the import totals
     */
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

    /**
     * Call the directory exporter {@code Export} RPC.
     *
     * @param r the export request
     * @return the stream of export responses
     */
    public Multi<ExportResponse> export(ExportRequest r) {
        return multi(this::exporter, s -> s.export(r));
    }
}
