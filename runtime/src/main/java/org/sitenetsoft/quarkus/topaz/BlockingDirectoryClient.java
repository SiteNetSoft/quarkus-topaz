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
 * Blocking view of {@link DirectoryClient}.
 * Each call waits at most the configured deadline plus one second, or 30 seconds if no deadline is set.
 * Failures, including an exceeded wait, surface as {@link io.grpc.StatusRuntimeException}.
 * Do not call it on an event-loop thread.
 */
public final class BlockingDirectoryClient {

    private final DirectoryClient delegate;
    private final Duration timeout;

    /**
     * Create a blocking view of a directory client.
     *
     * @param delegate the reactive client to wrap
     */
    BlockingDirectoryClient(DirectoryClient delegate) {
        this.delegate = delegate;
        this.timeout = BlockingTopazClient.waitFor(delegate.deadline());
    }

    private <T> T await(Uni<T> uni) {
        return BlockingTopazClient.await(uni, timeout);
    }

    /**
     * Block until {@link DirectoryClient#check} completes.
     * Fail closed: an error never yields {@code true}.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param objectType the object type
     * @param objectId the object id
     * @param relation the relation name
     * @param subjectType the subject type
     * @param subjectId the subject id
     * @return {@code true} if the check passes
     */
    public boolean check(String objectType, String objectId, String relation, String subjectType, String subjectId) {
        return await(delegate.check(objectType, objectId, relation, subjectType, subjectId));
    }

    /**
     * Block until {@link DirectoryClient#checks} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param checks the checks to run
     * @return the results, in request order
     */
    public List<Boolean> checks(List<CheckRequest> checks) {
        return await(delegate.checks(checks));
    }

    /**
     * Block until {@link DirectoryClient#getObject} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param type the object type
     * @param id the object id
     * @return the object
     */
    public aserto.directory.common.v3.Object getObject(String type, String id) {
        return await(delegate.getObject(type, id));
    }

    /**
     * Block until {@link DirectoryClient#setObject} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param o the object to store
     * @return the stored object
     */
    public aserto.directory.common.v3.Object setObject(aserto.directory.common.v3.Object o) {
        return await(delegate.setObject(o));
    }

    /**
     * Block until {@link DirectoryClient#deleteObject} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param type the object type
     * @param id the object id
     * @param withRelations whether to delete the relations of the object too
     */
    public void deleteObject(String type, String id, boolean withRelations) {
        await(delegate.deleteObject(type, id, withRelations));
    }

    /**
     * Block until {@link DirectoryClient#getRelation} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param objectType the object type
     * @param objectId the object id
     * @param relation the relation name
     * @param subjectType the subject type
     * @param subjectId the subject id
     * @return the relation
     */
    public Relation getRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        return await(delegate.getRelation(objectType, objectId, relation, subjectType, subjectId));
    }

    /**
     * Block until {@link DirectoryClient#setRelation} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param r the relation to store
     * @return the stored relation
     */
    public Relation setRelation(Relation r) {
        return await(delegate.setRelation(r));
    }

    /**
     * Block until {@link DirectoryClient#deleteRelation} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param objectType the object type
     * @param objectId the object id
     * @param relation the relation name
     * @param subjectType the subject type
     * @param subjectId the subject id
     */
    public void deleteRelation(String objectType, String objectId, String relation, String subjectType,
            String subjectId) {
        await(delegate.deleteRelation(objectType, objectId, relation, subjectType, subjectId));
    }

    /**
     * Block until {@link DirectoryClient#getManifest} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @return the manifest YAML
     */
    public String getManifest() {
        return await(delegate.getManifest());
    }

    /**
     * Block until {@link DirectoryClient#setManifest} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param yaml the manifest YAML
     */
    public void setManifest(String yaml) {
        await(delegate.setManifest(yaml));
    }

    /**
     * Block until {@link DirectoryClient#deleteManifest} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     */
    public void deleteManifest() {
        await(delegate.deleteManifest());
    }

    /**
     * Block until {@link DirectoryClient#importData(Multi)} completes.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param requests the import requests
     * @return the import totals
     */
    public ImportSummary importData(Multi<ImportRequest> requests) {
        return await(delegate.importData(requests));
    }

    /**
     * Import the given requests through {@link DirectoryClient#importData}.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param requests the import requests
     * @return the import totals
     */
    public ImportSummary importData(List<ImportRequest> requests) {
        return importData(Multi.createFrom().iterable(requests));
    }

    /**
     * Collect the whole export stream into a list.
     * The wait is bounded by the configured deadline.
     * Failures surface as {@link io.grpc.StatusRuntimeException}.
     *
     * @param r the export request
     * @return all export responses
     */
    public List<ExportResponse> export(ExportRequest r) {
        return await(delegate.export(r).collect().asList());
    }
}
