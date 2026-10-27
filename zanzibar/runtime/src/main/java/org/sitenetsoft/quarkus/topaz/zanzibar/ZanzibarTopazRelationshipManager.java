package org.sitenetsoft.quarkus.topaz.zanzibar;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.zanzibar.runtime.TopazZanzibarConfig;

import aserto.directory.common.v3.Relation;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkiverse.zanzibar.Relationship;
import io.quarkiverse.zanzibar.RelationshipManager;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Implements {@link RelationshipManager} on the Topaz Directory.
 * <p>
 * The manager fails closed. A {@code NOT_FOUND} answer to a check means "not related" ({@code false}). The method
 * {@link #remove} ignores a {@code NOT_FOUND} for a relation that does not exist. Every other failure propagates as a
 * {@link StatusRuntimeException} and never yields {@code true}.
 * <p>
 * The methods {@link #add} and {@link #remove} are not atomic. They call the Directory once per element, in order.
 * They stop at the first failure, and the elements before it stay applied.
 */
@ApplicationScoped
public class ZanzibarTopazRelationshipManager implements RelationshipManager {

    private final DirectoryClient directory;
    private final TopazZanzibarConfig config;

    /**
     * Create the manager.
     *
     * @param directory the client for the Topaz Directory
     * @param config the connector configuration
     */
    @Inject
    ZanzibarTopazRelationshipManager(DirectoryClient directory, TopazZanzibarConfig config) {
        this.directory = directory;
        this.config = config;
    }

    /**
     * Check the relationship with a Directory {@code Check}, which evaluates relations and permissions alike.
     * A {@code NOT_FOUND} answers {@code false}. Other failures propagate.
     */
    @Override
    public Uni<Boolean> check(Relationship r) {
        Relation rel = toRelation(r, config.defaultUserType());
        return directory.check(rel.getObjectType(), rel.getObjectId(), rel.getRelation(), rel.getSubjectType(),
                rel.getSubjectId())
                .onFailure(ZanzibarTopazRelationshipManager::isNotFound).recoverWithItem(false);
    }

    /**
     * Set each relationship with a Directory {@code SetRelation}, in order.
     * Setting an existing relation succeeds.
     * The call is not atomic. The first failure propagates and the earlier elements stay set.
     */
    @Override
    public Uni<Void> add(List<Relationship> rs) {
        return Multi.createFrom().iterable(rs)
                .onItem().transformToUniAndConcatenate(r -> directory
                        .setRelation(toRelation(r, config.defaultUserType())).replaceWithVoid())
                .collect().last().replaceWithVoid();
    }

    /**
     * Delete each relationship with a Directory {@code DeleteRelation}, in order.
     * A relation that does not exist is ignored.
     * The call is not atomic. The first other failure propagates and the earlier elements stay removed.
     */
    @Override
    public Uni<Void> remove(List<Relationship> rs) {
        return Multi.createFrom().iterable(rs)
                .onItem().transformToUniAndConcatenate(r -> {
                    Relation rel = toRelation(r, config.defaultUserType());
                    return directory.deleteRelation(rel.getObjectType(), rel.getObjectId(), rel.getRelation(),
                            rel.getSubjectType(), rel.getSubjectId())
                            .onFailure(ZanzibarTopazRelationshipManager::isNotFound).recoverWithNull();
                })
                .collect().last().replaceWithVoid();
    }

    /**
     * Copy a relationship field by field into a Directory relation.
     * A blank user type falls back to {@code defaultUserType}.
     *
     * @param r the relationship to convert
     * @param defaultUserType the subject type for a relationship with a blank user type
     * @return the Directory relation
     */
    static Relation toRelation(Relationship r, String defaultUserType) {
        String userType = r.userType().isBlank() ? defaultUserType : r.userType();
        return Relation.newBuilder()
                .setObjectType(r.objectType()).setObjectId(r.objectId()).setRelation(r.relation())
                .setSubjectType(userType).setSubjectId(r.userId())
                .build();
    }

    /**
     * Test whether a failure is a gRPC {@code NOT_FOUND} status.
     *
     * @param t the failure to test
     * @return {@code true} when {@code t} is a {@link StatusRuntimeException} with status {@code NOT_FOUND}
     */
    static boolean isNotFound(Throwable t) {
        return t instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.NOT_FOUND;
    }
}
