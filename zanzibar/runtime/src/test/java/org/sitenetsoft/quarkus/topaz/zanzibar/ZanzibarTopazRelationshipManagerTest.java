package org.sitenetsoft.quarkus.topaz.zanzibar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import aserto.directory.common.v3.Relation;
import io.grpc.Status;
import io.quarkiverse.zanzibar.Relationship;

class ZanzibarTopazRelationshipManagerTest {

    @Test
    void copiesAllFields() {
        Relation rel = ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "user", "rick@the-citadel.com"), "user");
        assertThat(rel.getObjectType()).isEqualTo("todo");
        assertThat(rel.getObjectId()).isEqualTo("todo-1");
        assertThat(rel.getRelation()).isEqualTo("owner");
        assertThat(rel.getSubjectType()).isEqualTo("user");
        assertThat(rel.getSubjectId()).isEqualTo("rick@the-citadel.com");
        assertThat(rel.getSubjectRelation()).isEmpty();
    }

    @Test
    void blankUserTypeFallsBackToDefault() {
        assertThat(ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "", "rick@the-citadel.com"), "user").getSubjectType())
                .isEqualTo("user");
        assertThat(ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "  ", "rick@the-citadel.com"), "account").getSubjectType())
                .isEqualTo("account");
    }

    @Test
    void explicitUserTypeWins() {
        assertThat(ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "group", "admins"), "user").getSubjectType())
                .isEqualTo("group");
    }

    @Test
    void notFoundDetection() {
        assertThat(ZanzibarTopazRelationshipManager.isNotFound(Status.NOT_FOUND.asRuntimeException())).isTrue();
        assertThat(ZanzibarTopazRelationshipManager.isNotFound(Status.UNAVAILABLE.asRuntimeException())).isFalse();
        assertThat(ZanzibarTopazRelationshipManager.isNotFound(new IllegalStateException())).isFalse();
    }
}
