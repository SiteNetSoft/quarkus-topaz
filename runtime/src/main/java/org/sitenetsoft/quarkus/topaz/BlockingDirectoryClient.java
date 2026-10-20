package org.sitenetsoft.quarkus.topaz;

import java.time.Duration;
import java.util.List;

import aserto.directory.common.v3.Relation;
import aserto.directory.exporter.v3.ExportRequest;
import aserto.directory.exporter.v3.ExportResponse;
import aserto.directory.importer.v3.ImportRequest;
import aserto.directory.reader.v3.CheckRequest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Blocking view of {@link DirectoryClient}. Each call waits at most the configured deadline plus one second (30 s if none);
 * failures, including an exceeded wait, surface as {@link io.grpc.StatusRuntimeException}.
 * Must not be called on an event-loop thread.
 */
public final class BlockingDirectoryClient {

    private final DirectoryClient delegate;
    private final Duration timeout;

    BlockingDirectoryClient(DirectoryClient delegate) {
        this.delegate = delegate;
        this.timeout = BlockingTopazClient.waitFor(delegate.deadline());
    }

    private <T> T await(Uni<T> uni) {
        return BlockingTopazClient.await(uni, timeout);
    }

    /**
     * Blocking form of {@link DirectoryClient#check}; fails closed, never returning {@code true} on an error. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public boolean check(String objectType, String objectId, String relation, String subjectType, String subjectId) {
        return await(delegate.check(objectType, objectId, relation, subjectType, subjectId));
    }

    /**
     * Blocking form of {@link DirectoryClient#checks}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public List<Boolean> checks(List<CheckRequest> checks) {
        return await(delegate.checks(checks));
    }

    /**
     * Blocking form of {@link DirectoryClient#getObject}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public aserto.directory.common.v3.Object getObject(String type, String id) {
        return await(delegate.getObject(type, id));
    }

    /**
     * Blocking form of {@link DirectoryClient#setObject}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public aserto.directory.common.v3.Object setObject(aserto.directory.common.v3.Object o) {
        return await(delegate.setObject(o));
    }

    /**
     * Blocking form of {@link DirectoryClient#deleteObject}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public void deleteObject(String type, String id, boolean withRelations) {
        await(delegate.deleteObject(type, id, withRelations));
    }

    /**
     * Blocking form of {@link DirectoryClient#getRelation}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public Relation getRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return await(delegate.getRelation(objectType, objectId, relation, subjectType, subjectId));
    }

    /**
     * Blocking form of {@link DirectoryClient#setRelation}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public Relation setRelation(Relation r) {
        return await(delegate.setRelation(r));
    }

    /**
     * Blocking form of {@link DirectoryClient#deleteRelation}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public void deleteRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        await(delegate.deleteRelation(objectType, objectId, relation, subjectType, subjectId));
    }

    /**
     * Blocking form of {@link DirectoryClient#getManifest}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public String getManifest() {
        return await(delegate.getManifest());
    }

    /**
     * Blocking form of {@link DirectoryClient#setManifest}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public void setManifest(String yaml) {
        await(delegate.setManifest(yaml));
    }

    /**
     * Blocking form of {@link DirectoryClient#deleteManifest}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public void deleteManifest() {
        await(delegate.deleteManifest());
    }

    /**
     * Blocking form of {@link DirectoryClient#importData}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public ImportSummary importData(Multi<ImportRequest> requests) {
        return await(delegate.importData(requests));
    }

    /**
     * Imports the given requests through {@link DirectoryClient#importData}. Blocks for at most the configured deadline. Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public ImportSummary importData(List<ImportRequest> requests) {
        return importData(Multi.createFrom().iterable(requests));
    }

    /** Collects the whole export stream into a list. */
    public List<ExportResponse> export(ExportRequest r) {
        return await(delegate.export(r).collect().asList());
    }
}
