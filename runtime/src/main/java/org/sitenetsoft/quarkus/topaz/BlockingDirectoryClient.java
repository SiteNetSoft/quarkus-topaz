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
 * Blocking view of {@link DirectoryClient}. Each call waits at most the configured deadline (30 s if none).
 * Must not be called on an event-loop thread.
 */
public final class BlockingDirectoryClient {

    private final DirectoryClient delegate;
    private final Duration timeout;

    BlockingDirectoryClient(DirectoryClient delegate) {
        this.delegate = delegate;
        this.timeout = delegate.deadline().orElse(BlockingTopazClient.DEFAULT_TIMEOUT);
    }

    private <T> T await(Uni<T> uni) {
        return uni.await().atMost(timeout);
    }

    public boolean check(String objectType, String objectId, String relation, String subjectType, String subjectId) {
        return await(delegate.check(objectType, objectId, relation, subjectType, subjectId));
    }

    public List<Boolean> checks(List<CheckRequest> checks) {
        return await(delegate.checks(checks));
    }

    public aserto.directory.common.v3.Object getObject(String type, String id) {
        return await(delegate.getObject(type, id));
    }

    public aserto.directory.common.v3.Object setObject(aserto.directory.common.v3.Object o) {
        return await(delegate.setObject(o));
    }

    public void deleteObject(String type, String id, boolean withRelations) {
        await(delegate.deleteObject(type, id, withRelations));
    }

    public Relation getRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return await(delegate.getRelation(objectType, objectId, relation, subjectType, subjectId));
    }

    public Relation setRelation(Relation r) {
        return await(delegate.setRelation(r));
    }

    public void deleteRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        await(delegate.deleteRelation(objectType, objectId, relation, subjectType, subjectId));
    }

    public String getManifest() {
        return await(delegate.getManifest());
    }

    public void setManifest(String yaml) {
        await(delegate.setManifest(yaml));
    }

    public void deleteManifest() {
        await(delegate.deleteManifest());
    }

    public ImportSummary importData(Multi<ImportRequest> requests) {
        return await(delegate.importData(requests));
    }

    public ImportSummary importData(List<ImportRequest> requests) {
        return importData(Multi.createFrom().iterable(requests));
    }

    /** Collects the whole export stream into a list. */
    public List<ExportResponse> export(ExportRequest r) {
        return await(delegate.export(r).collect().asList());
    }
}
