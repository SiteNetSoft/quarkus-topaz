# quarkus-topaz-zanzibar Implementation Plan

**Goal:** Ship `quarkus-topaz-zanzibar`: a `io.quarkiverse.zanzibar.RelationshipManager` backed by the Topaz Directory, so applications written against `quarkus-zanzibar`'s `@FGA*` annotations run on Topaz unchanged.

**Architecture:** Two new reactor modules, `zanzibar/runtime` (`quarkus-topaz-zanzibar`) and `zanzibar/deployment` (`quarkus-topaz-zanzibar-deployment`). One `@ApplicationScoped` bean, `ZanzibarTopazRelationshipManager`, maps `check` to Reader `Check`, `add` to Writer `SetRelation` and `remove` to Writer `DeleteRelation` through the existing `DirectoryClient`; the deployment module registers the feature and the bean. `quarkus-zanzibar` itself owns the JAX-RS filter, the annotations, user extraction and its own `quarkus.zanzibar.*` configuration; this module adds exactly one key, the user type to use when the filter hands over a blank one.

**Tech Stack:** Quarkus 3.40.1, `io.quarkiverse.zanzibar:quarkus-zanzibar` 2.15.0 (built against Quarkus 3.39.4; `main` is on 3.40.1), Mutiny, `quarkus-topaz` client, Dev Service Topaz 0.33.22 for tests, `quarkus-security-test-utils` for test identities.

**Spec:** `docs/design/specs/2026-10-18-quarkus-topaz-design.md`, section 7 (plus sections 2, 3, 8, 9). The client plan `docs/design/plans/2026-10-18-quarkus-topaz-client.md` describes the `DirectoryClient` API this plan consumes; the security plan `docs/design/plans/2026-10-19-quarkus-topaz-security.md` is the layout template (module poms, test fixtures, docs wiring).

## Global Constraints

- Quarkus `3.40.1`; `quarkus.version` property; `io.quarkus:quarkus-bom` imported; nothing else version-pinned except what the BOM does not manage. Two such pins in this plan: `io.quarkiverse.zanzibar:quarkus-zanzibar` and its `-deployment` artifact at `2.15.0` through a root property `quarkus-zanzibar.version`, and `io.quarkus:quarkus-security-test-utils:${quarkus.version}` (test scope).
- GroupId `org.sitenetsoft`, version `0.1.0-SNAPSHOT`, no license headers in files (LICENSE file only).
- `maven.compiler.release=21`; build with `JAVA_HOME=/path/to/jdk-25`.
- Topaz image `ghcr.io/aserto-dev/topaz:0.33.22`. No dependency on any `com.aserto:*` artifact.
- `quarkus-topaz-zanzibar` depends on `quarkus-topaz`, `quarkus-arc` and `io.quarkiverse.zanzibar:quarkus-zanzibar`. It is the only module that touches Quarkiverse artifacts; `quarkus-topaz` and `quarkus-topaz-security` must not gain one.
- Container runtime is rootless Podman: every test or build command that starts containers runs with `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`.
- Maven parallelism at most `-T 4` locally; one heavy job at a time; native builds always pass `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`; commit before any native build.
- Module-scoped builds use `-pl zanzibar/deployment -am` (building `zanzibar/runtime` alone fails the extension-descriptor check, as it does for `security/runtime`).
- Fail closed: `check` never completes with `true` on an error. A `NOT_FOUND` from the Directory on `check` means "no such relationship" and completes with `false`; every other `StatusRuntimeException` propagates unchanged (quarkus-zanzibar's filter turns it into a 500, which is its decision, not ours).
- Package roots: public API in `org.sitenetsoft.quarkus.topaz.zanzibar`, config in `org.sitenetsoft.quarkus.topaz.zanzibar.runtime`, build steps in `org.sitenetsoft.quarkus.topaz.zanzibar.deployment`.
- Feature name `topaz-zanzibar`. Config prefix `quarkus.topaz.zanzibar`. Extension name `Topaz Zanzibar`.
- Test identities: `quarkus.http.auth.basic=true` with `TestIdentityController`/`TestIdentityProvider`; users `rick@the-citadel.com` (owner of `todo-1`) and `morty@the-citadel.com` exist in the directory, `summer@the-citadel.com` does not. Directory fixtures are the security module's (`manifest.yaml`, `objects.json`, `relations.json`) plus a `todo-2` object with no relations.

## Review Focus

1. `quarkus-zanzibar` builds a `Relationship` with user type `""` when the endpoint has no `@FGAUserType` and the user came from `quarkus.zanzibar.filter.unauthenticated-user`; Topaz rejects an empty `subject_type`. The connector must substitute `quarkus.topaz.zanzibar.default-user-type` (default `user`) so such checks still answer. Pinned by `RelationshipManagerTest.blankUserTypeUsesDefault` (Task 2).
2. Adding the connector to an application turns on `quarkus-zanzibar`'s filter with `deny-unannotated-resource-methods=true`: every unannotated REST endpoint starts answering 403. The docs must say so on the first screen and the sample app must set the property. Pinned by `FgaFilterTest.unannotatedEndpointIsDenied` (Task 3).
3. When Topaz is unreachable the filter must never answer 200; with a client deadline configured it answers 500 within that deadline plus the filter's own slack. Pinned by `FgaFilterTopazDownTest` (Task 3).
4. `remove` of a relationship that does not exist, and `add` of one that already exists, must both complete normally (spec section 7: SetRelation is idempotent, missing relation ignored). Pinned by `RelationshipManagerTest.addIsIdempotent` and `removeMissingRelationIsIgnored` (Task 2).
5. A caller that is authenticated in Quarkus but unknown to the Directory must get 403, not 500. Pinned by `FgaFilterTest.unknownUserIsForbidden` (Task 3).

---

### Task 1: Module scaffold, feature and config root

**Files:**
- Create: `zanzibar/pom.xml` (artifact `quarkus-topaz-zanzibar-parent`, packaging `pom`, name `Topaz Zanzibar - Parent`, modules `runtime`, `deployment`)
- Create: `zanzibar/runtime/pom.xml` (artifact `quarkus-topaz-zanzibar`, name `Topaz Zanzibar - Runtime`; deps `quarkus-topaz` `${project.version}`, `quarkus-arc`, `io.quarkiverse.zanzibar:quarkus-zanzibar:${quarkus-zanzibar.version}`; test deps `junit-jupiter`, `assertj-core`; the `quarkus-extension-maven-plugin` descriptor and `quarkus-extension-processor` blocks copied from `security/runtime/pom.xml`)
- Create: `zanzibar/deployment/pom.xml` (artifact `quarkus-topaz-zanzibar-deployment`, name `Topaz Zanzibar - Deployment`; deps `quarkus-topaz-deployment`, `quarkus-arc-deployment`, `io.quarkiverse.zanzibar:quarkus-zanzibar-deployment:${quarkus-zanzibar.version}`, `quarkus-topaz-zanzibar`; test deps as `security/deployment/pom.xml`: `quarkus-junit-internal`, `quarkus-rest-deployment`, `quarkus-rest-jackson-deployment`, `rest-assured`, `assertj-core`, `io.quarkus:quarkus-security-test-utils:${quarkus.version}`)
- Create: `zanzibar/runtime/src/main/resources/META-INF/quarkus-extension.yaml` (name `Topaz Zanzibar`, description `Topaz Directory connector for Quarkus Zanzibar's RelationshipManager`, artifactId `quarkus-topaz-zanzibar`, same keywords, categories, status, guide as `security/runtime`'s yaml, config `quarkus.topaz.zanzibar.`)
- Create: `zanzibar/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/zanzibar/runtime/TopazZanzibarConfig.java`
- Create: `zanzibar/deployment/src/main/java/org/sitenetsoft/quarkus/topaz/zanzibar/deployment/TopazZanzibarProcessor.java`
- Modify: `pom.xml` (root): `<module>zanzibar</module>` after `security`; property `<quarkus-zanzibar.version>2.15.0</quarkus-zanzibar.version>` next to `assertj.version`
- Modify: `docs/pom.xml`: add dependency `quarkus-topaz-zanzibar-deployment` `${project.version}` after the security one, so the generated config reference includes the new key
- Test: `zanzibar/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/zanzibar/deployment/TopazZanzibarFeatureTest.java`

**Interfaces:**
- Produces: `TopazZanzibarConfig` (`@ConfigMapping(prefix = "quarkus.topaz.zanzibar") @ConfigRoot(phase = RUN_TIME)`): `@WithDefault("user") String defaultUserType()` with Javadoc "Subject type used when quarkus-zanzibar hands over a relationship whose user type is blank (no `@FGAUserType` on the endpoint)."
- Produces: `TopazZanzibarProcessor.FEATURE = "topaz-zanzibar"`; a `@BuildStep` producing `FeatureBuildItem(FEATURE)`. The bean registration step is added in Task 2.

- [ ] **Step 1: Write the failing test**

```java
public class TopazZanzibarFeatureTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.port", "1")
            .overrideConfigKey("quarkus.topaz.directory.port", "1")
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.zanzibar.filter.enabled", "false");

    @Inject TopazZanzibarConfig config;

    @Test
    void defaultUserType() {
        assertThat(config.defaultUserType()).isEqualTo("user");
    }

    @Test
    void featureIsInstalled() {
        assertThat(TopazZanzibarProcessor.FEATURE).isEqualTo("topaz-zanzibar");
    }

    @Test
    void zanzibarCoreIsOnTheClasspath() {
        // the connector is useless without quarkus-zanzibar; a wrong artifactId in the pom would still compile this module
        assertThat(io.quarkiverse.zanzibar.RelationshipManager.class).isInterface();
    }
}
```

`quarkus.zanzibar.filter.enabled=false` keeps `quarkus-zanzibar` from looking up a `RelationshipManager` that Task 2 has not written yet.

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/path/to/jdk-25 mvn -q -T 4 -pl zanzibar/deployment -am test -Dtest=TopazZanzibarFeatureTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation failure on `TopazZanzibarConfig` (or BUILD FAILURE because the module does not exist yet).

- [ ] **Step 3: Create the three poms, the yaml, the config interface and the processor with the feature step; register the module and the version property in the root pom; add the docs dependency**

- [ ] **Step 4: Run test to verify it passes**

Run: the Step 2 command.
Expected: Tests run: 3, Failures: 0. `zanzibar/runtime/target/classes/META-INF/quarkus-extension.properties` names `org.sitenetsoft:quarkus-topaz-zanzibar-deployment:0.1.0-SNAPSHOT` as the deployment artifact.

- [ ] **Step 5: Commit**

```bash
git add pom.xml docs/pom.xml zanzibar
git commit -m "Scaffold quarkus-topaz-zanzibar: feature, config root and module wiring"
```

---

### Task 2: `ZanzibarTopazRelationshipManager`

**Files:**
- Create: `zanzibar/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/zanzibar/ZanzibarTopazRelationshipManager.java`
- Modify: `zanzibar/deployment/.../TopazZanzibarProcessor.java`: `@BuildStep` producing `AdditionalBeanBuildItem.unremovableOf(ZanzibarTopazRelationshipManager.class)`
- Test: `zanzibar/runtime/src/test/java/org/sitenetsoft/quarkus/topaz/zanzibar/ZanzibarTopazRelationshipManagerTest.java` (pure mapping)
- Test: `zanzibar/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/zanzibar/deployment/RelationshipManagerTest.java` (against the Dev Service)
- Create test fixtures: `zanzibar/deployment/src/test/resources/topaz/manifest.yaml`, `topaz/data/objects.json`, `topaz/data/relations.json` copied from `security/deployment/src/test/resources/topaz/`, with `{"type": "todo", "id": "todo-2", "display_name": "Fix portal gun"}` appended to `objects.json`; `topaz/policies/public.rego` copied as well (the Dev Service needs a policy directory)

**Interfaces:**
- Consumes: `org.sitenetsoft.quarkus.topaz.DirectoryClient`: `Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType, String subjectId)`, `Uni<Relation> setRelation(Relation)`, `Uni<Void> deleteRelation(String objectType, String objectId, String relation, String subjectType, String subjectId)`; `aserto.directory.common.v3.Relation` builder (`setObjectType`, `setObjectId`, `setRelation`, `setSubjectType`, `setSubjectId`); `io.quarkiverse.zanzibar.Relationship(objectType, objectId, relation, userType, userId)`; `TopazZanzibarConfig.defaultUserType()`.
- Produces: `@ApplicationScoped public class ZanzibarTopazRelationshipManager implements RelationshipManager` with constructor `@Inject ZanzibarTopazRelationshipManager(DirectoryClient directory, TopazZanzibarConfig config)` and
  - `Uni<Boolean> check(Relationship r)`: `directory.check(...)` on the mapped relation; `onFailure(StatusRuntimeException with Status.Code.NOT_FOUND).recoverWithItem(false)`; no other recovery.
  - `Uni<Void> add(List<Relationship> rs)`: one `setRelation` per element, run in order (`Multi.createFrom().iterable(rs).onItem().transformToUniAndConcatenate(...)` then `.collect().last().replaceWithVoid()`); empty list completes immediately; errors propagate.
  - `Uni<Void> remove(List<Relationship> rs)`: one `deleteRelation` per element in order; a `NOT_FOUND` on an element is recovered as success for that element; other errors propagate.
  - `static Relation toRelation(Relationship r, String defaultUserType)`: field-by-field copy; `subject_type` is `r.userType()` unless it is blank, then `defaultUserType`.
  - `static boolean isNotFound(Throwable t)`: `t instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.NOT_FOUND`.

- [ ] **Step 1: Write the failing unit tests (runtime module)**

```java
class ZanzibarTopazRelationshipManagerTest {
    @Test void copiesAllFields() {
        Relation rel = ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "user", "rick@the-citadel.com"), "user");
        assertThat(rel.getObjectType()).isEqualTo("todo");
        assertThat(rel.getObjectId()).isEqualTo("todo-1");
        assertThat(rel.getRelation()).isEqualTo("owner");
        assertThat(rel.getSubjectType()).isEqualTo("user");
        assertThat(rel.getSubjectId()).isEqualTo("rick@the-citadel.com");
        assertThat(rel.getSubjectRelation()).isEmpty();
    }
    @Test void blankUserTypeFallsBackToDefault() {
        assertThat(ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "", "rick@the-citadel.com"), "user").getSubjectType())
                .isEqualTo("user");
        assertThat(ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "  ", "rick@the-citadel.com"), "account").getSubjectType())
                .isEqualTo("account");
    }
    @Test void explicitUserTypeWins() {
        assertThat(ZanzibarTopazRelationshipManager.toRelation(
                Relationship.of("todo", "todo-1", "owner", "group", "admins"), "user").getSubjectType())
                .isEqualTo("group");
    }
    @Test void notFoundDetection() {
        assertThat(ZanzibarTopazRelationshipManager.isNotFound(Status.NOT_FOUND.asRuntimeException())).isTrue();
        assertThat(ZanzibarTopazRelationshipManager.isNotFound(Status.UNAVAILABLE.asRuntimeException())).isFalse();
        assertThat(ZanzibarTopazRelationshipManager.isNotFound(new IllegalStateException())).isFalse();
    }
}
```

- [ ] **Step 2: Write the failing deployment tests**

```java
public class RelationshipManagerTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.zanzibar.filter.enabled", "false");

    @Inject RelationshipManager manager;          // the quarkus-zanzibar interface, resolved to our bean

    static final Relationship RICK_OWNS_1 = Relationship.of("todo", "todo-1", "owner", "user", "rick@the-citadel.com");
    static final Relationship MORTY_OWNS_1 = Relationship.of("todo", "todo-1", "owner", "user", "morty@the-citadel.com");
    static final Relationship MORTY_OWNS_2 = Relationship.of("todo", "todo-2", "owner", "user", "morty@the-citadel.com");

    @Test void beanIsOurs() { assertThat(ClientProxy.unwrap(manager)).isInstanceOf(ZanzibarTopazRelationshipManager.class); }
    @Test void seededRelationshipChecksTrue()  { assertThat(await(manager.check(RICK_OWNS_1))).isTrue(); }
    @Test void absentRelationshipChecksFalse() { assertThat(await(manager.check(MORTY_OWNS_1))).isFalse(); }
    @Test void permissionChecksLikeARelation() {
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "can_delete", "user", "rick@the-citadel.com")))).isTrue();
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "can_delete", "user", "morty@the-citadel.com")))).isFalse();
    }
    @Test void unknownUserChecksFalse() {
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "owner", "user", "summer@the-citadel.com")))).isFalse();
    }
    @Test void blankUserTypeUsesDefault() {
        assertThat(await(manager.check(Relationship.of("todo", "todo-1", "owner", "", "rick@the-citadel.com")))).isTrue();
    }
    @Test void addThenCheckThenRemove() {
        await(manager.add(List.of(MORTY_OWNS_2)));
        assertThat(await(manager.check(MORTY_OWNS_2))).isTrue();
        await(manager.remove(List.of(MORTY_OWNS_2)));
        assertThat(await(manager.check(MORTY_OWNS_2))).isFalse();
    }
    @Test void addIsIdempotent() {
        await(manager.add(List.of(RICK_OWNS_1)));          // already seeded
        await(manager.add(List.of(RICK_OWNS_1, RICK_OWNS_1)));
        assertThat(await(manager.check(RICK_OWNS_1))).isTrue();
    }
    @Test void removeMissingRelationIsIgnored() {
        await(manager.remove(List.of(Relationship.of("todo", "todo-2", "owner", "user", "rick@the-citadel.com"))));
    }
    @Test void emptyListsComplete() { await(manager.add(List.of())); await(manager.remove(List.of())); }
    @Test void unknownTypeOnAddPropagates() {
        assertThatThrownBy(() -> await(manager.add(List.of(Relationship.of("spaceship", "s-1", "owner", "user", "rick@the-citadel.com")))))
                .isInstanceOf(StatusRuntimeException.class);
    }
    @Test void unknownTypeOnCheckPropagates() {
        assertThatThrownBy(() -> await(manager.check(Relationship.of("spaceship", "s-1", "owner", "user", "rick@the-citadel.com"))))
                .isInstanceOf(StatusRuntimeException.class);
    }
    static <T> T await(Uni<T> u) { return u.await().atMost(Duration.ofSeconds(10)); }
}
```

If Topaz answers `NOT_FOUND` rather than `INVALID_ARGUMENT` for an unknown object type on `check`, `unknownTypeOnCheckPropagates` cannot hold as written: change its assertion to `isFalse()` and record in the commit message which status Topaz 0.33.22 returns. `unknownTypeOnAddPropagates` must hold either way (writes do not recover `NOT_FOUND`).

- [ ] **Step 3: Run tests to verify they fail**

Run: `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/path/to/jdk-25 mvn -q -T 4 -pl zanzibar/deployment -am verify -Dtest='ZanzibarTopazRelationshipManagerTest,RelationshipManagerTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation failure on `ZanzibarTopazRelationshipManager`.

- [ ] **Step 4: Implement the bean and the processor's bean step**

- [ ] **Step 5: Run tests to verify they pass**

Run: the Step 3 command, then the Task 1 feature test with `quarkus.zanzibar.filter.enabled` override removed (the bean now exists, the filter may be on; `featureIsInstalled` must still pass).
Expected: runtime 4 tests, deployment 12 + 3 tests, all green.

- [ ] **Step 6: Commit**

```bash
git add zanzibar
git commit -m "Add ZanzibarTopazRelationshipManager over the Topaz Directory"
```

---

### Task 3: The `@FGARelation` flow end to end in the deployment tests

**Files:**
- Test fixtures: `zanzibar/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/zanzibar/deployment/FgaTodoResource.java`, `TestIdentities.java` (copy of the security module's)
- Test: `zanzibar/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/zanzibar/deployment/FgaFilterTest.java`
- Test: `zanzibar/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/zanzibar/deployment/FgaFilterTopazDownTest.java`

**Interfaces:**
- Consumes: `quarkus-zanzibar` annotations `io.quarkiverse.zanzibar.annotations.FGAPathObject(param, type)`, `FGARelation(value)` with constant `FGARelation.ANY`, `FGAUserType(value)`, `FGAIgnore`; `io.quarkiverse.zanzibar.RelationshipManager`; the Task 2 bean through the interface.
- Produces: nothing new in `main`.

`FgaTodoResource` (`@Path("/fga/todos")`, `@ApplicationScoped`, class-level `@FGAUserType("user")`):

| Method | Path | FGA annotations | Body |
|---|---|---|---|
| `GET` | `/{id}` | `@FGAPathObject(param = "id", type = "todo") @FGARelation("owner")` | returns `{"id": "<id>"}` |
| `DELETE` | `/{id}` | `@FGAPathObject(param = "id", type = "todo") @FGARelation("can_delete")` | 204 |
| `PUT` | `/{id}/owners/{user}` | `@FGAPathObject(param = "id", type = "todo") @FGARelation(FGARelation.ANY)` | `manager.add(List.of(Relationship.of("todo", id, "owner", "user", user)))`, returns `Uni<Response>` 204 |
| `DELETE` | `/{id}/owners/{user}` | same as PUT | `manager.remove(...)`, 204 |
| `GET` | `/unannotated` | none | returns `"open"` |
| `GET` | `/ignored` | `@FGAIgnore` | returns `"ignored"` |

- [ ] **Step 1: Write the failing filter test**

```java
public class FgaFilterTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(FgaTodoResource.class, TestIdentities.class, TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data");

    @BeforeAll static void identities() { TestIdentities.register(); }

    @Test void ownerCanRead()        { rick().get("/fga/todos/todo-1").then().statusCode(200).body("id", equalTo("todo-1")); }
    @Test void nonOwnerIsForbidden() { morty().get("/fga/todos/todo-1").then().statusCode(403); }
    @Test void unknownUserIsForbidden() { summer().get("/fga/todos/todo-1").then().statusCode(403); }
    @Test void anonymousIsForbidden()   { given().get("/fga/todos/todo-1").then().statusCode(403); }   // quarkus-zanzibar denies without a principal; it does not challenge
    @Test void permissionRelation() {
        rick().delete("/fga/todos/todo-1").then().statusCode(204);
        morty().delete("/fga/todos/todo-1").then().statusCode(403);
    }
    @Test void shareAndUnshare() {
        morty().get("/fga/todos/todo-2").then().statusCode(403);
        rick().put("/fga/todos/todo-2/owners/morty@the-citadel.com").then().statusCode(204);
        morty().get("/fga/todos/todo-2").then().statusCode(200);
        rick().delete("/fga/todos/todo-2/owners/morty@the-citadel.com").then().statusCode(204);
        morty().get("/fga/todos/todo-2").then().statusCode(403);
    }
    @Test void unannotatedEndpointIsDenied() { rick().get("/fga/todos/unannotated").then().statusCode(403); }
    @Test void ignoredEndpointIsOpen()       { given().get("/fga/todos/ignored").then().statusCode(200).body(equalTo("ignored")); }
    // rick()/morty()/summer(): given().auth().preemptive().basic(user, password)
}
```

- [ ] **Step 2: Write the failing Topaz-down test**

Copy `security/deployment/.../RoutePolicyTopazDownTest`'s silent `ServerSocket` setup. Config: Dev Service disabled, both hosts `127.0.0.1`, both ports the silent socket, `quarkus.topaz.plain-text=true`, `quarkus.topaz.deadline=1s`, basic auth as above.

```java
@Test void neverPermitsWhenTopazIsDown() {
    long t0 = System.nanoTime();
    int status = rick().get("/fga/todos/todo-1").then().extract().statusCode();
    Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
    assertThat(status).isIn(500, 503);                       // quarkus-zanzibar maps a failed check to InternalServerErrorException
    assertThat(elapsed).isLessThan(Duration.ofSeconds(5));   // the 1s client deadline fires before the filter's 5s timeout
}
```

- [ ] **Step 3: Run to verify they fail**

Run: `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -q -T 4 -pl zanzibar/deployment -am verify -Dtest='FgaFilterTest,FgaFilterTopazDownTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation failure on `FgaTodoResource`.

- [ ] **Step 4: Write `FgaTodoResource` and `TestIdentities`**

- [ ] **Step 5: Run to verify they pass**

Run: the Step 3 command.
Expected: 8 + 1 tests green. If `anonymousIsForbidden` gets 401 instead of 403, keep the test's expectation as whatever Quarkus 3.40.1 and quarkus-zanzibar 2.15.0 actually return and note the value in the commit message; the docs (Task 5) state it.

- [ ] **Step 6: Commit**

```bash
git add zanzibar/deployment/src/test
git commit -m "Exercise the quarkus-zanzibar filter against Topaz in the deployment tests"
```

---

### Task 4: Integration tests, JVM and native

**Files:**
- Modify: `integration-tests/pom.xml`: dependency `quarkus-topaz-zanzibar` `${project.version}` after `quarkus-topaz-security`; a `quarkus-topaz-zanzibar-deployment` `pom`/`test` reactor-ordering dependency alongside the existing deployment ones
- Modify: `integration-tests/src/main/resources/application.properties`: `quarkus.zanzibar.filter.deny-unannotated-resource-methods=false` with a comment that the todo app's other endpoints are secured by the `topaz` policy, not by FGA annotations
- Modify: `integration-tests/src/main/resources/topaz/data/objects.json`: add `{"type": "todo", "id": "todo-2", "display_name": "Fix portal gun"}` (no relations; the seed currently has only `todo-1`)
- Create: `integration-tests/src/main/java/org/sitenetsoft/quarkus/topaz/it/FgaTodoResource.java`: the Task 3 resource minus `/unannotated` and `/ignored`, at `@Path("/fga/todos")`
- Test: `integration-tests/src/test/java/org/sitenetsoft/quarkus/topaz/it/FgaTodoResourceTest.java` (`@QuarkusTest`), `FgaTodoResourceIT.java` (`@QuarkusIntegrationTest extends FgaTodoResourceTest`)

**Interfaces:**
- Consumes: embedded users from the existing `application.properties` (`rick`, `morty`, `summer` with their names as passwords); the existing Dev Service seed.

- [ ] **Step 1: Write the failing test**

Same six behaviours as `FgaFilterTest` minus the two unannotated/ignored cases, plus:

```java
@Test void existingEndpointsAreUnaffected() {
    given().get("/todos").then().statusCode(200);            // not FGA-annotated; deny-unannotated is off in this app
    rick().get("/secured/todos").then().statusCode(200);     // still guarded by the topaz HTTP policy
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -q -T 4 -pl integration-tests -am verify -Dtest=FgaTodoResourceTest -Dsurefire.failIfNoSpecifiedTests=false -DskipITs`
Expected: compilation failure on `FgaTodoResource`.

- [ ] **Step 3: Add the resource, the pom dependencies, the property and the object**

- [ ] **Step 4: JVM run**

Run: the Step 2 command.
Expected: 7 tests green, and the existing `SecuredTodoResourceTest`/`TodoResourceTest` still green when run without `-Dtest`.

- [ ] **Step 5: Commit, then native run**

```bash
git add integration-tests
git commit -m "Add the FGA-annotated todo endpoints to the integration tests"
```

Run (one heavy job, nothing else building): `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -pl integration-tests -am verify -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8 2>&1 | tee target/logs/zanzibar-native.log`
Expected: BUILD SUCCESS; Failsafe runs `FgaTodoResourceIT` 7/7 alongside the existing ITs. If the native image fails on a missing reflection registration for a `quarkus-zanzibar` class, add a `ReflectiveClassBuildItem` for it in `TopazZanzibarProcessor` and commit that separately.

---

### Task 5: Documentation

**Files:**
- Create: `docs/modules/ROOT/pages/zanzibar-connector.adoc`: what it is (one paragraph: quarkus-zanzibar's annotations, Topaz as the backend); installation snippet; **the first admonition**: adding the artifact enables `quarkus-zanzibar`'s filter and `deny-unannotated-resource-methods` defaults to `true`, so unannotated REST endpoints answer 403 until you annotate them, add `@FGAIgnore`, or set the property to `false`; the mapping table (`check` → Reader `Check`, `add` → Writer `SetRelation`, idempotent, `remove` → Writer `DeleteRelation`, missing ignored, subject relation never set); user identity (principal name is the subject id; `@FGAUserType` or `quarkus.topaz.zanzibar.default-user-type` is the subject type; the user must exist in the directory or checks are false); permissions can be named in `@FGARelation` because Topaz `Check` evaluates both; the `@FGARelation` walkthrough using the todo manifest and `FgaTodoResource` from `integration-tests` (owner read, `can_delete`, share/unshare through `RelationshipManager`); error behaviour (anonymous and unknown users get 403; a Topaz error or deadline gives the filter's 500; `quarkus.topaz.deadline` bounds the wait); the one config key, with a pointer to the quarkus-zanzibar docs for `quarkus.zanzibar.*`; a note that this module is a candidate for upstreaming as a quarkiverse connector
- Modify: `docs/modules/ROOT/nav.adoc` (entry `Zanzibar connector` after `Security`), `docs/modules/ROOT/pages/index.adoc` (artifact table row for `quarkus-topaz-zanzibar`; the intro sentence mentions the optional connector), `docs/modules/ROOT/pages/configuration.adoc` (include the generated `quarkus-topaz-zanzibar.adoc` as the security one is included), `docs/modules/ROOT/pages/native.adoc` and `testing.adoc` (one line each, as the security plan added), `README.md` (one paragraph and the artifact snippet)
- Verify: `.github/workflows/ci.yml` needs no change (`build` runs the whole reactor, `native` builds `integration-tests -am`)

- [ ] **Step 1: Write the page and the edits**

- [ ] **Step 2: Build the docs**

Run: `JAVA_HOME=/path/to/jdk-25 mvn -q -T 4 -Pdocs -pl docs -am package -DskipTests`
Expected: `docs/target/generated-docs/zanzibar-connector.html` exists; `grep -c "quarkus.topaz.zanzibar.default-user-type" docs/target/generated-docs/configuration.html` ≥ 1.

- [ ] **Step 3: Full build**

Run: `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/path/to/jdk-25 mvn -T 4 verify -Pdocs 2>&1 | tee target/logs/zanzibar-verify.log`
Expected: BUILD SUCCESS across all modules; no `relocated`, `argLine` or `java.util.logging.manager` warnings.

- [ ] **Step 4: Commit**

```bash
git add docs README.md
git commit -m "Document quarkus-topaz-zanzibar"
```

---

## Decisions recorded in this plan (not in the spec)

- `quarkus-zanzibar` 2.15.0 is pinned by a root property; it is built against Quarkus 3.39.4 and its `main` is on 3.40.1, so the minor skew is accepted and the pin is bumped by PR like the Topaz image.
- `quarkus.topaz.zanzibar.default-user-type` exists because the filter can hand over a blank user type; the spec's "subject relation is left empty" is kept as is.
- `check` recovers `NOT_FOUND` as `false`; writes do not recover it except `remove`, where the spec says a missing relation is ignored.
- The filter's own behaviour (403 for anonymous, 500 on a failed check, `deny-unannotated-resource-methods=true`) is documented, not changed: this module must stay a drop-in connector so it can be upstreamed.
- The share and unshare endpoints require `@FGARelation("owner")`, not `FGARelation.ANY` as the endpoint table above says: quarkus-zanzibar's filter passes `ANY` before it requires a user or checks anything, so anonymous callers could grant or revoke ownership. Fixed in the final review; the tests use `todo-3`, owned by rick, and cover non-owner and anonymous share/unshare (403).
- `quarkus.zanzibar.filter.timeout`, not the client's `quarkus.topaz.deadline`, bounds a check on a stalled Topaz connection; the deadline was observed not to end such a call. A refused connection fails fast with `UNAVAILABLE`.
- User type: quarkus-zanzibar resolves it from the `type:id` principal name, then a single role, then `@FGAUserType` last. The docs therefore recommend `quarkus.zanzibar.extract-user-type-from-roles=false` and `quarkus.zanzibar.extract-user-type-from-name=false` together with `@FGAUserType("user")`; `quarkus.topaz.zanzibar.default-user-type` only fills a blank type (the `unauthenticated-user` path without `@FGAUserType`, or a direct `RelationshipManager` call).
- Every Surefire test runs with `quarkus.http.test-port=0` (root `pluginManagement`), so modules running at once under `-T 4` do not clash on 8081.
