package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.zanzibar.ZanzibarTopazRelationshipManager;

import org.sitenetsoft.quarkus.topaz.DirectoryClient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkiverse.zanzibar.Relationship;
import io.quarkiverse.zanzibar.RelationshipManager;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class RelationshipManagerTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.zanzibar.filter.enabled", "false");

    @Inject
    RelationshipManager manager;

    @Inject
    DirectoryClient directory;

    static final Relationship RICK_OWNS_1 = Relationship.of("todo", "todo-1", "owner", "user", "rick@the-citadel.com");
    static final Relationship MORTY_OWNS_1 = Relationship.of("todo", "todo-1", "owner", "user", "morty@the-citadel.com");
    static final Relationship MORTY_OWNS_2 = Relationship.of("todo", "todo-2", "owner", "user", "morty@the-citadel.com");

    @Test
    void beanIsOurs() {
        assertThat(ClientProxy.unwrap(manager)).isInstanceOf(ZanzibarTopazRelationshipManager.class);
    }

    @Test
    void seededRelationshipChecksTrue() {
        assertThat(await(manager.check(RICK_OWNS_1))).isTrue();
    }

    @Test
    void absentRelationshipChecksFalse() {
        assertThat(await(manager.check(MORTY_OWNS_1))).isFalse();
    }

    @Test
    void permissionChecksLikeARelation() {
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "can_delete", "user", "rick@the-citadel.com")))).isTrue();
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "can_delete", "user", "morty@the-citadel.com")))).isFalse();
    }

    @Test
    void unknownRelationChecksFalse() {
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "nope", "user", "rick@the-citadel.com")))).isFalse();
    }

    @Test
    void unknownUserChecksFalse() {
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "owner", "user", "summer@the-citadel.com")))).isFalse();
    }

    @Test
    void blankUserTypeUsesDefault() {
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "owner", "", "rick@the-citadel.com")))).isTrue();
    }

    @Test
    void addThenCheckThenRemove() {
        await(manager.add(List.of(MORTY_OWNS_2)));
        assertThat(await(manager.check(MORTY_OWNS_2))).isTrue();
        await(manager.remove(List.of(MORTY_OWNS_2)));
        assertThat(await(manager.check(MORTY_OWNS_2))).isFalse();
    }

    @Test
    void addIsIdempotent() {
        await(manager.add(List.of(RICK_OWNS_1)));
        await(manager.add(List.of(RICK_OWNS_1, RICK_OWNS_1)));
        assertThat(await(manager.check(RICK_OWNS_1))).isTrue();
    }

    @Test
    void removeMissingRelationIsIgnored() {
        await(manager.remove(List.of(Relationship.of("todo", "todo-2", "owner", "user", "rick@the-citadel.com"))));
    }

    @Test
    void directoryDeleteOfMissingRelationCompletes() {
        await(directory.deleteRelation("todo", "todo-2", "owner", "user", "rick@the-citadel.com"));
    }

    @Test
    void emptyListsComplete() {
        await(manager.add(List.of()));
        await(manager.remove(List.of()));
    }

    @Test
    void unknownTypeOnAddPropagates() {
        assertThatThrownBy(() -> await(manager.add(List.of(Relationship.of("spaceship", "s-1", "owner", "user", "rick@the-citadel.com")))))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> assertThat(e.getStatus().getCode())
                        .isEqualTo(Status.Code.INVALID_ARGUMENT).isNotEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void mixedListStopsAtFirstFailure() {
        Relationship spaceship = Relationship.of("spaceship", "s-1", "owner", "user", "rick@the-citadel.com");
        assertThatThrownBy(() -> await(manager.add(List.of(RICK_OWNS_1, spaceship))))
                .isInstanceOf(StatusRuntimeException.class);
        assertThatThrownBy(() -> await(manager.add(List.of(spaceship, MORTY_OWNS_2))))
                .isInstanceOf(StatusRuntimeException.class);
        assertThat(await(manager.check(MORTY_OWNS_2))).isFalse();
    }

    @Test
    void unknownTypeOnCheckIsFalse() {
        // Topaz itself answers check=false here (see directoryCheckOnUnknownTypeIsFalse); no NOT_FOUND recovery involved
        assertThat(await(manager.check(Relationship.of("spaceship", "s-1", "owner", "user", "rick@the-citadel.com"))))
                .isFalse();
    }

    @Test
    void directoryCheckOnUnknownTypeIsFalse() {
        // Topaz 0.33.22 answers OK with check=false (no gRPC error) for an unknown object type
        assertThat(await(directory.check("spaceship", "s-1", "owner", "user", "rick@the-citadel.com"))).isFalse();
    }

    static <T> T await(Uni<T> u) {
        return u.await().atMost(Duration.ofSeconds(10));
    }
}
