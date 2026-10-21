# Pre-release fixes (0.1.0) Implementation Plan

**Goal:** Close the fix-later findings parked by the client, security and zanzibar plans so that 0.1.0 ships with bounded waits everywhere, a deliberate public API, no secrets in logs, honest tests, and a spec that matches the code.

**Architecture:** No new modules. Seven tasks, each scoped to one concern across the existing modules: bounded waits in the client (gRPC deadline on stalled connections, health and Dev UI timeouts); correctness fixes in the security evaluator and path mapper; the client's public API sweep; the security test gaps; the zanzibar and client test gaps; build and CI housekeeping; documentation and spec drift. Behaviour changes come first so the docs task at the end describes the final state.

**Tech Stack:** Quarkus 3.40.1, Mutiny, gRPC over the Vert.x gRPC client, `QuarkusUnitTest` with the Topaz Dev Service, Antora docs.

**Spec:** `docs/design/specs/2026-10-18-quarkus-topaz-design.md` (sections 4, 5, 7, 8, 9). The findings themselves come from the final reviews recorded in `docs/design/plans/2026-10-18-quarkus-topaz-client.md`, `docs/design/plans/2026-10-19-quarkus-topaz-security.md` and `docs/design/plans/2026-10-19-quarkus-topaz-zanzibar.md` and are restated in each task below; the plan is self-contained.

## Global Constraints

- Quarkus `3.40.1`; `quarkus.version` property; `io.quarkus:quarkus-bom` imported; nothing else version-pinned except what the BOM does not manage (`quarkus-zanzibar` 2.15.0, `quarkus-security-test-utils`, and after Task 6 the Jandex Maven plugin through a root property).
- GroupId `org.sitenetsoft`, version `0.1.0-SNAPSHOT`, no license headers in files.
- `maven.compiler.release=21`; build with `JAVA_HOME=/path/to/jdk-25`.
- Topaz image `ghcr.io/aserto-dev/topaz:0.33.22`; no `com.aserto:*` dependency; no `grpc-netty-shaded`.
- Container runtime is rootless Podman: every test or build command that starts containers runs with `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`.
- Maven parallelism at most `-T 4` locally; one heavy job at a time; native builds always pass `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`; commit before any native build. The host logged machine-check events on 2026-10-19 during builds: keep to one build at a time.
- Fail closed: no convenience method returns `true` on an error; client errors are `io.grpc.StatusRuntimeException`; every security denial logs at WARN naming the policy path.
- Public API after this plan: in `org.sitenetsoft.quarkus.topaz`: `TopazClient` and its `Builder` (except `settings`, which is internal), `AuthorizerClient`, `DirectoryClient`, `BlockingTopazClient`, `BlockingAuthorizerClient`, `BlockingDirectoryClient`, `IdentityContexts`, `Structs`, `ImportSummary`; in `org.sitenetsoft.quarkus.topaz.security`: everything already there; in `org.sitenetsoft.quarkus.topaz.zanzibar`: `ZanzibarTopazRelationshipManager`. Every `*.runtime` and `*.deployment` package is internal and says so in a `package-info.java`.
- Module-scoped builds: `-pl deployment -am`, `-pl security/deployment -am`, `-pl zanzibar/deployment -am`, `-pl integration-tests -am`.
- Logs and scratch files under `target/logs/`, never `/tmp`.

## Review Focus

1. A gRPC call to a Topaz that accepts the connection and never answers must fail with `DEADLINE_EXCEEDED` when `quarkus.topaz.deadline` is set, within the deadline plus one second. Pinned by `DeadlineOnStalledConnectionTest` (Task 1).
2. An `IdentityMapper` or `ResourceContextMapper` whose `map` call itself blocks must not hang the request past `quarkus.topaz.enforcement.timeout`; the request is denied with 403. Pinned by `RoutePolicySlowMapperTest` (Task 2).
3. An application that provides its own `PolicyPathMapper` must start without `quarkus.topaz.enforcement.policy-root`. Pinned by `RoutePolicyCustomMapperNoRootTest` (Task 2).
4. A denial's WARN line must not carry the subject (an e-mail address today); the subject goes to DEBUG. Pinned by `RoutePolicyTest.denialLogDoesNotNameSubject` (Task 2).
5. A JAX-RS template segment with a regex containing `/`, such as `{path:.+/.+}`, must map to one `__path` segment, not several. Pinned by `PolicyPathsTest.regexWithSlash` (Task 2).

---

### Task 1: Bounded waits in the client

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/DirectoryClient.java` (`uni`/`multi` helpers, lines 83-89), `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/AuthorizerClient.java` (its call helper)
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/health/TopazHealthCheck.java:32-45`, `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/devui/TopazJsonRpcService.java` (`listPolicies`, `getManifest`, and any other `Uni`-returning method)
- Modify: `docs/modules/ROOT/pages/client.adoc:86-90` (deadline paragraph) and `docs/modules/ROOT/pages/zanzibar-connector.adoc` (Errors section: the sentence saying the deadline was observed not to end a stalled call)
- Modify: `zanzibar/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/zanzibar/deployment/FgaFilterTopazDownTest.java` (see Step 5)
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/DeadlineOnStalledConnectionTest.java`, `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/RefusedConnectionTest.java`, `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/TopazHealthDownTest.java` (extend), `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/devui/TopazDevUITest.java` (no change needed unless Step 7 says so)

**Interfaces:**
- Consumes: `DirectoryClient.check(...)`, `AuthorizerClient.info()`, `TopazClient` beans; the silent-`ServerSocket` technique from `security/deployment/.../RoutePolicyTopazDownTest.java`.
- Produces: every `Uni`/`Multi` returned by `AuthorizerClient` and `DirectoryClient` fails with `StatusRuntimeException` whose status code is `DEADLINE_EXCEEDED` once the configured deadline elapses without an item, regardless of transport state; `TopazHealthCheck.call()` and each `TopazJsonRpcService` method complete within `HEALTH_TIMEOUT = Duration.ofSeconds(10)` (down / failure) when no deadline is configured.

- [ ] **Step 1: Write the failing stalled-connection test**

```java
public class DeadlineOnStalledConnectionTest {
    private static final ServerSocket SILENT = silentListener();   // copy from RoutePolicyTopazDownTest
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.deadline", "1s");
    @Inject TopazClient client;

    @Test void directoryCheckFailsAtDeadline() {
        long t0 = System.nanoTime();
        assertThatThrownBy(() -> client.directory().check("todo", "todo-1", "owner", "user", "rick@the-citadel.com")
                .await().atMost(Duration.ofSeconds(10)))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isBetween(Duration.ofSeconds(1), Duration.ofSeconds(3));
    }
    @Test void authorizerInfoFailsAtDeadline()  { /* same shape with client.authorizer().info() */ }
    @Test void manifestStreamFailsAtDeadline()  { /* same shape with client.directory().getManifest() (a Multi-backed Uni) */ }
    @Test void blockingFacadeFailsAtDeadline()  { /* client.blocking().directory().check(...) throws StatusRuntimeException DEADLINE_EXCEEDED within 3s */ }
}
```

- [ ] **Step 2: Run it**

Run: `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/path/to/jdk-25 mvn -q -T 4 -pl deployment -am verify -Dtest=DeadlineOnStalledConnectionTest -Dsurefire.failIfNoSpecifiedTests=false`

Two outcomes, both handled:
- **FAIL** (the call never completes; `await` times out after 10 s): this confirms the zanzibar finding. Go to Step 3.
- **PASS** without a code change: the gRPC deadline does fire in isolation and the zanzibar observation had another cause. Skip Step 3; in Step 5 investigate `FgaFilterTopazDownTest` (candidates: the `quarkus.topaz.deadline` key not reaching the client because the Dev Service is disabled after config is read, or quarkus-zanzibar subscribing on a thread where the deadline is not propagated) and record the finding in the commit message. Steps 4, 6 and 7 still apply.

- [ ] **Step 3: Guard every call with the deadline in the client**

In `DirectoryClient.uni` and `multi`, and the equivalent in `AuthorizerClient`: when `deadline` is present, append `.ifNoItem().after(deadline).failWith(() -> Status.DEADLINE_EXCEEDED.withDescription("Topaz call exceeded the configured deadline of " + deadline).asRuntimeException())` for `Uni`, and for `Multi` the same on the first item (`.ifNoItem().after(deadline).failWith(...)` applies to the first item in Mutiny). The helpers become instance methods (they need `deadline`). Make sure the blocking facade's existing deadline-plus-one-second wait still maps the failure to `DEADLINE_EXCEEDED` (it already does).

- [ ] **Step 4: Write the refused-connection test**

```java
public class RefusedConnectionTest {   // a closed port: find a free port with new ServerSocket(0) and close it before start
    // same config as above but pointing at the closed port; no deadline
    @Test void checkFailsFastWithUnavailable() {
        long t0 = System.nanoTime();
        assertThatThrownBy(() -> client.directory().check(...).await().atMost(Duration.ofSeconds(10)))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isLessThan(Duration.ofSeconds(5));
    }
}
```

This pins the sentence the zanzibar docs already state. If the observed code is not `UNAVAILABLE`, assert the observed code and fix the docs sentence accordingly.

- [ ] **Step 5: Health and Dev UI timeouts**

`TopazHealthCheck.call()`: add `.ifNoItem().after(HEALTH_TIMEOUT).fail()` before the `.map(...)`, so a stalled Topaz reports DOWN within 10 s without a configured deadline. `TopazJsonRpcService`: same guard on each `Uni` method, failing with an `IllegalStateException("Topaz did not answer within 10s")`. Extend `TopazHealthDownTest` with `stalledTopazIsDownWithinTenSeconds` using a silent socket and no deadline: `/q/health/ready` answers 503 and the elapsed time is between 10 s and 15 s. Then update `FgaFilterTopazDownTest` per Step 2's outcome: if Step 3 was applied, the 1 s client deadline now fires first, so set its expectations to the deadline (elapsed in [1 s, 4 s), the log record predicate matches the filter's ERROR with `DEADLINE_EXCEEDED` or `TimeoutException`), and delete the comment saying the deadline does not fire.

- [ ] **Step 6: Run the covering tests**

Run: the Step 2 command with `-Dtest='DeadlineOnStalledConnectionTest,RefusedConnectionTest,TopazHealthDownTest,TopazHealthCheckTest,TopazBeansTest'`, then `-pl zanzibar/deployment -am ... -Dtest=FgaFilterTopazDownTest`, then `-pl security/deployment -am ... -Dtest='RoutePolicyTopazDownTest,TopazCheckTopazDownTest'` (they must keep passing: their enforcement timeout of 2 s is shorter than any deadline they set).
Expected: all green.

- [ ] **Step 7: Docs**

`client.adoc` deadline paragraph: state that the deadline is enforced by the client itself when the connection stalls (after Step 3) and that a refused connection fails immediately with `UNAVAILABLE`; health and Dev UI calls give up after 10 s without a deadline. `zanzibar-connector.adoc`: remove the "observed not to end a stalled call" sentence; say which of the client deadline and `quarkus.zanzibar.filter.timeout` fires first (the shorter one).

- [ ] **Step 8: Commit**

```bash
git add runtime deployment zanzibar/deployment docs
git commit -m "Enforce the configured deadline on stalled connections and bound health and Dev UI calls"
```

---

### Task 2: Security evaluator and path mapper fixes

**Files:**
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/DecisionEvaluator.java:92-120` (defer mapper calls; WARN wording), `security/runtime/.../runtime/PolicyPaths.java` (brace-aware splitting), `security/runtime/.../runtime/DefaultPolicyPathMapper.java` (strip `quarkus.rest.path` too), `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/TopazRelation.java:38-57` (reject whitespace), `security/runtime/.../runtime/TopazCheckInterceptor.java:64` and `TopazPermissionAugmentor.java:75,90` (WARN wording)
- Modify: `security/deployment/src/main/java/org/sitenetsoft/quarkus/topaz/security/deployment/TopazSecurityProcessor.java:211-240` (custom mapper detection; `isJaxrsResource` message)
- Test: `security/runtime/src/test/.../PolicyPathsTest.java`, `DefaultPolicyPathMapperTest.java`, `security/runtime/src/test/java/org/sitenetsoft/quarkus/topaz/security/TopazRelationTest.java` (new), `security/deployment/src/test/.../RoutePolicySlowMapperTest.java` (new), `RoutePolicyCustomMapperNoRootTest.java` (new), `RoutePolicyTest.java` (extend), `TopazAuthorizeNonEndpointBuildTest.java` (extend)

**Interfaces:**
- Produces: `DecisionEvaluator.evaluateRequest` runs the three mappers inside `Uni.createFrom().deferred(...)` so the `ifNoItem().after(timeout)` guard bounds them; denial WARN lines have the form `Topaz denied <policyPath> for <identity type>: <cause>` and a DEBUG line `Topaz denied <policyPath> for <identity type> <subject>` carries the subject. `PolicyPaths.of` splits on `/` only at brace depth zero. `DefaultPolicyPathMapper` strips `quarkus.http.root-path` and then `quarkus.rest.path` (default `/`) from the raw path. `TopazRelation` rejects any whitespace in the name with `IllegalArgumentException`. `TopazSecurityProcessor.validate` passes `policyRootRequired=false` and skips the permission scan when the index contains a non-default `PolicyPathMapper` bean (a class implementing the interface other than `DefaultPolicyPathMapper`), by handing the recorder a second boolean `customPathMapper`.

- [ ] **Step 1: Write the failing unit tests**

```java
// PolicyPathsTest
@Test void regexWithSlash() { assertThat(PolicyPaths.of("r", "GET", "/files/{path:.+/.+}/meta")).isEqualTo("r.GET.files.__path.meta"); }
@Test void nestedBracesInRegex() { assertThat(PolicyPaths.of("r", "GET", "/a/{id:[0-9]{3}}")).isEqualTo("r.GET.a.__id"); }
// DefaultPolicyPathMapperTest (constructor gains a third parameter restPath)
@Test void stripsRestPathInRawMode() { /* rootPath "/", restPath "/api", request path "/api/todos" → "todoApp.GET.todos" */ }
@Test void stripsRootThenRestPath() { /* rootPath "/app", restPath "/api", request "/app/api/todos" → "todoApp.GET.todos" */ }
// TopazRelationTest
@Test void rejectsWhitespace() {
    for (String bad : List.of("todo #owner", "todo#own er", "todo#owner@ user", " todo#owner")) {
        assertThatThrownBy(() -> new TopazRelation(bad, "todo-1")).isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Write the failing deployment tests**

`RoutePolicySlowMapperTest`: same setup as `RoutePolicyTest` plus a test bean `SlowIdentityMapper implements IdentityMapper` whose `map` sleeps 5 s before returning (a blocking body, not a delayed `Uni`); `quarkus.topaz.enforcement.timeout=1s`. Test: `rick().get("/todos")` answers 403 within 4 s; the WARN log names `todoApp.GET.todos`.

`RoutePolicyCustomMapperNoRootTest`: `RoutePolicyTest` setup minus the `policy-root` key, plus a test bean `FixedPolicyPathMapper implements PolicyPathMapper` returning `"todoApp.GET.todos"`. Test: the app starts (no `ConfigurationException`) and `rick().get("/todos")` answers 200.

`RoutePolicyTest.denialLogDoesNotNameSubject`: with `setLogRecordPredicate` on WARN records from `DecisionEvaluator`, `summer().get("/todos")` → 403 and no WARN record's formatted message contains `summer@the-citadel.com`, while one contains `todoApp.GET.todos`. (Add the predicate to the existing class or create `RoutePolicyDenialLogTest` if the class already has a predicate for another purpose.)

`TopazAuthorizeNonEndpointBuildTest`: add an interface-layout case: `@Path` on an interface, the empty-path `@TopazAuthorize` on the implementing class's method that lacks an HTTP-method annotation; the build error message names the class and says the method needs an HTTP method annotation (fix `isJaxrsResource`'s message if it currently says the class is not a resource).

- [ ] **Step 3: Run to verify they fail**

Run: `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -q -T 4 -pl security/deployment -am verify -Dtest='PolicyPathsTest,DefaultPolicyPathMapperTest,TopazRelationTest,RoutePolicySlowMapperTest,RoutePolicyCustomMapperNoRootTest,RoutePolicyTest,TopazAuthorizeNonEndpointBuildTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: the new tests fail (compile error for the new mapper constructor parameter; `RoutePolicySlowMapperTest` hangs ~5 s then gets 403 late or 200; `RoutePolicyCustomMapperNoRootTest` fails at startup).

- [ ] **Step 4: Implement**

- `DecisionEvaluator.evaluateRequest`: move the whole body after the policy-path block into `Uni.createFrom().deferred(() -> ...)` and keep the `ifNoItem().after(config.timeout())` outside it. Note: a mapper that blocks the event loop still blocks it; the test's sleeping mapper runs on a worker because `HttpSecurityPolicy` checks run on the event loop only for non-blocking policies; if the test shows the event loop blocked (request never answered), run the mapper call on `Infrastructure.getDefaultWorkerPool()` via `runSubscriptionOn` and say so in the Javadoc.
- `describe(identity)`: WARN gets the identity type only; add `LOG.debugf` with the subject.
- `PolicyPaths.of`: iterate characters tracking `{`/`}` depth; split on `/` at depth 0.
- `DefaultPolicyPathMapper`: inject `@ConfigProperty(name = "quarkus.rest.path", defaultValue = "/") String restPath`; strip after the root path.
- `TopazRelation`: `name.chars().anyMatch(Character::isWhitespace)` → `malformed(name)`.
- `TopazSecurityProcessor.validate`: scan the index for implementors of `PolicyPathMapper` other than `DefaultPolicyPathMapper` (and other than interfaces/abstract classes); pass `customPathMapper` to `recorder.validate(boolean policyRootRequired, boolean customPathMapper)`, which returns early when it is true.

- [ ] **Step 5: Run to verify they pass**

Run: the Step 3 command, then the whole `security/deployment` module without `-Dtest`.
Expected: all green; existing `RoutePolicyMissingRootTest` still fails the build when the default mapper is in use.

- [ ] **Step 6: Commit**

```bash
git add security
git commit -m "Bound mapper calls by the enforcement timeout, keep subjects out of WARN logs, and fix path-mapping edge cases"
```

---

### Task 3: Client public API sweep

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/TopazClient.java` (Javadoc on every public method; `Builder.settings` Javadoc marked internal), `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/TopazChannels.java` (`useTls`), `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/TopazRecorder.java:36-80`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/package-info.java`, `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/package-info.java`, `zanzibar/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/zanzibar/runtime/package-info.java`, each: "Internal implementation of the extension. Classes in this package are not API and may change in any release."
- Modify: `runtime/src/test/java/org/sitenetsoft/quarkus/topaz/runtime/TopazChannelsTest.java` (per Step 2)
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/topaz/PublicApiTest.java` (new)

**Interfaces:**
- Produces: the public-API list in Global Constraints is exact. `ApiKeyClientInterceptor` moves to `org.sitenetsoft.quarkus.topaz` as a package-private class (its test moves with it). `TopazChannels` stays public (the recorder in the same package and `TopazClient.Builder.build()` in the parent package both need it) but its `package-info` marks it internal; `useTls` is either the recorder's single TLS decision or deleted (Step 2).

- [ ] **Step 1: Write the failing API test**

```java
class PublicApiTest {
    @Test void onlyIntendedTypesArePublicInTheApiPackage() throws Exception {
        // list classes in org.sitenetsoft.quarkus.topaz (non-recursive) via the classpath directory of TopazClient
        Set<String> publicTypes = ...;   // simple names of public top-level classes
        assertThat(publicTypes).containsExactlyInAnyOrder("TopazClient", "AuthorizerClient", "DirectoryClient",
                "BlockingTopazClient", "BlockingAuthorizerClient", "BlockingDirectoryClient", "IdentityContexts",
                "Structs", "ImportSummary");
    }
    @Test void everyPublicMethodOfTopazClientHasJavadoc() // skip: Javadoc is not visible at runtime; instead:
    @Test void runtimePackageIsMarkedInternal() { assertThat(TopazChannels.class.getPackage().getAnnotation(Deprecated.class)).isNull(); /* placeholder removed in Step 3 */ }
}
```

Replace the second and third tests with one real check: `assertThat(Class.forName("org.sitenetsoft.quarkus.topaz.runtime.package-info")).isNotNull()` fails until the file exists (javac emits `package-info.class` only when the package has annotations or Javadoc; annotate the package with `@io.quarkus.runtime.annotations.RegisterForReflection(targets = {})`? No: annotate it with a tiny marker annotation `@Internal` you create in `org.sitenetsoft.quarkus.topaz.runtime` as `@Documented @Retention(RUNTIME) @Target(PACKAGE) public @interface Internal {}`, and test `Package.getPackage(...).isAnnotationPresent(Internal.class)` for all three runtime packages).

- [ ] **Step 2: `useTls`**

Read `TopazRecorder.resolveTls` and `TopazChannels.useTls`. If the recorder's decision is equivalent (plain-text / named TLS / default), replace the recorder's logic with a call to `TopazChannels.useTls` so there is one decision point and keep `TopazChannelsTest`'s `useTls` tests. If it is not equivalent, delete `useTls` and its tests. Either way, the TLS behaviour tests in `deployment` (`TopazTlsJvmDefaultTest`, `TopazTlsMissingTest`) must still pass.

- [ ] **Step 3: Implement the sweep**

Move `ApiKeyClientInterceptor` (+ test) to the API package as package-private; create the `@Internal` annotation and the three `package-info.java` files; mark `Builder.settings` Javadoc "Internal: used by the extension's recorder. Not API." and keep it public (the recorder is in another package); write Javadoc for every public method of `TopazClient`, `AuthorizerClient`, `DirectoryClient` and the blocking facades that lacks it (one sentence each; convenience methods say which RPC they call and that failures are `StatusRuntimeException`).

- [ ] **Step 4: Run**

Run: `JAVA_HOME=... mvn -q -T 4 -pl runtime test` then `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -q -T 4 -pl deployment -am verify -Dtest='TopazTlsJvmDefaultTest,TopazTlsMissingTest,TopazBeansTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: green; `PublicApiTest` passes.

- [ ] **Step 5: Commit**

```bash
git add runtime security/runtime zanzibar/runtime
git commit -m "Mark runtime packages internal, hide the API-key interceptor, and complete the client Javadoc"
```

---

### Task 4: Security test gaps

**Files:**
- Test (all under `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/`): `TopazAuthorizeTest.java` (extend), `TopazCheckTest.java` (extend), `PermissionsAllowedTopazDownTest.java` (new), `RoutePolicyMissingRootTest.java` (extend or sibling `AuthorizationPolicyMissingRootTest.java`), `ReadBodyTest.java` (new), `TopazCheckAnonymousEvaluateTest.java` (new), fixtures `ServiceResource.java`/`SecuredService.java` (add methods)
- Test: `security/runtime/src/test/.../DefaultSubjectMapperTest.java` (blank JWT subject)

**Interfaces:**
- Consumes: the fixtures and identities of the security module; `TestIdentities`; the silent-socket pattern.
- Produces: nothing in `main`. If a test exposes a defect, fix it in the same task and name it in the commit message.

- [ ] **Step 1: Write the tests, one per gap, each with the assertion given**

| Test | Fixture | Assertion |
|---|---|---|
| `TopazAuthorizeTest.multiReturningMethodIsChecked` | `SecuredService.streamTodos()` returns `Multi<String>` under `@TopazAuthorize(path="todoApp.GET.todos")` exposed by a `ServiceResource` endpoint returning the `Multi` | rick 200 with the items; summer 403; the service method body is not entered for summer (a counter on the service stays unchanged) |
| `TopazAuthorizeTest.blockingMethodTimesOutTo403` | Topaz-down variant: silent socket, `timeout=1s`, blocking method | 403 within 4 s |
| `TopazAuthorizeTest.eventLoopCallerIsRejected` | a `@Route` handler (add `io.quarkus:quarkus-reactive-routes-deployment` as a test dependency of `security/deployment`) calling a blocking `@TopazAuthorize` method on the event loop | 500 and the log contains `EVENT_LOOP_MESSAGE`'s text |
| `TopazAuthorizeTest.classLevelBindingApplies` | `@TopazAuthorize(path=...)` on a class; a method without its own annotation | morty 403, rick 200 |
| `TopazCheckTest.classLevelBindingApplies` | same for `@TopazCheck` | same |
| `TopazCheckAnonymousEvaluateTest.anonymousIsStill401` | `anonymous=evaluate` | anonymous `@TopazCheck` call → 401 (the subject mapper cannot produce a subject) |
| `AuthorizationPolicyMissingRootTest` | `@AuthorizationPolicy(name="topaz")` on a resource, no `policy-root` | build/startup fails with the `ConfigurationException` message |
| `ReadBodyTest.bodyIsAvailableToMapper` | `quarkus.topaz.enforcement.read-body=true`, a test `ResourceContextMapper` that reads `request.body().asString()` into the Struct, a policy that allows when `input.resource.body == "yes"` | POST with body `yes` → 200; body `no` → 403; and a sibling test with `read-body` unset proves the body is empty in the mapper (403 for `yes`) |
| `PermissionsAllowedTopazDownTest` | bridge enabled, silent socket, `timeout=1s` | `@PermissionsAllowed("todoApp.GET.todos")` endpoint → 403 within 4 s; WARN "permission check timed out" logged |
| `TopazAuthorizeTest.decisionOverride` | `@TopazAuthorize(path=..., decision="visible")` where the policy sets `visible=true, allowed=false` for rick | rick 200; and with `decision="allowed"` rick 403 |
| `TopazAuthorizeNonEndpointBuildTest` interface case | covered in Task 2 | — |
| `DefaultSubjectMapperTest.blankJwtSubjectIsUnauthorized` | a `JsonWebToken` principal mock with `getSubject()` returning `" "` | `map` fails with `UnauthorizedException` |

- [ ] **Step 2: Run them**

Run: `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -q -T 4 -pl security/deployment -am verify -Dtest='TopazAuthorizeTest,TopazCheckTest,TopazCheckAnonymousEvaluateTest,AuthorizationPolicyMissingRootTest,ReadBodyTest,PermissionsAllowedTopazDownTest,DefaultSubjectMapperTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: green. A red test here is a defect in `main`: fix it minimally in the same task and record it in the commit message.

- [ ] **Step 3: Commit**

```bash
git add security
git commit -m "Cover the security module's untested paths: Multi dispatch, blocking timeouts, class bindings, read-body, bridge timeouts"
```

---

### Task 5: Zanzibar, client and Dev Service test gaps

**Files:**
- Test: `zanzibar/deployment/src/test/.../TopazZanzibarFeatureTest.java`, `RelationshipManagerTest.java`, `FgaFilterTopazDownTest.java`, `FgaFilterUnauthenticatedUserTest.java` (new), `integration-tests/src/test/.../FgaTodoResourceTest.java`, `TodoResourceTest.java`
- Test: `runtime/src/test/.../TopazClientTest.java` (`buildCreatesAndClosesChannels`), `deployment/src/test/.../devui/TopazDevUITest.java` (`connectionDescribesEndpoints`), `deployment/src/test/.../TopazHealthDownTest.java` (unreachable-host assertion), `deployment/src/test/.../devservices/DevServicesTopazPolicyDirAndImageTest.java` (new), `DevServicesTopazEmptyDataTest.java` (new)

**Interfaces:**
- Consumes: existing fixtures. Produces: nothing in `main` unless a test exposes a defect.

- [ ] **Step 1: Write the tests**

| Test | Assertion |
|---|---|
| `TopazZanzibarFeatureTest.featureIsInstalled` | replace the constant comparison with a log assertion: `setLogRecordPredicate(r -> r.getMessage().contains("Installed features"))` and `assertLogRecords` that the formatted message contains `topaz-zanzibar`, `topaz` and `zanzibar` (same for `TopazSecurityFeatureTest` and `TopazFeatureTest` if they share the tautology) |
| `RelationshipManagerTest.directoryDeleteOfMissingRelation` | call `directory.deleteRelation(...)` directly for a missing relation and assert the observed outcome (completes, or `StatusRuntimeException` with `NOT_FOUND`); name the test for what is observed; this pins which path `remove` takes |
| `RelationshipManagerTest.unknownTypeOnAddPropagates` | additionally assert `getStatus().getCode()` is the observed code and is not `NOT_FOUND` |
| `RelationshipManagerTest.mixedListStopsAtFirstFailure` | `add(List.of(RICK_OWNS_1 (valid), spaceship))` fails; `add(List.of(spaceship, MORTY_OWNS_2))` fails and `check(MORTY_OWNS_2)` is false afterwards |
| `FgaFilterTopazDownTest` | `String.valueOf(r.getLoggerName())` in the predicate |
| `FgaFilterUnauthenticatedUserTest` | `quarkus.zanzibar.filter.unauthenticated-user=rick@the-citadel.com`, `quarkus.zanzibar.extract-user-type-from-roles=false`, an endpoint `GET /fga/public/{id}` with `@FGAPathObject(param="id", type="todo") @FGARelation("owner")` and no `@FGAUserType`: anonymous GET `/fga/public/todo-1` → 200 (type filled by `default-user-type`), anonymous GET `/fga/public/todo-2` → 403; with `quarkus.topaz.zanzibar.default-user-type=group` the first call → 403 (positive control that the key is used) |
| `FgaTodoResourceTest.permissionRelation` | after the DELETE, `rick().get("/fga/todos/todo-1")` → 200 |
| `TodoResourceTest.missingUserParamIs400OrDenied`, `unknownTodoIdIsDenied` | `GET /todos/todo-1/can-delete` without `?user` → 400; `GET /todos/nope/can-delete?user=rick@…` → `{"allowed": false}` (fail closed, no 500) |
| `TopazClientTest.buildCreatesAndClosesChannels` | assert the two `ManagedChannelHolder`s' `GrpcClient`s are closed after `client.close()` (spy or a closed flag on the holder) rather than only that `build()` returned |
| `TopazDevUITest.connectionDescribesEndpoints` | assert `authorizer` equals `host:port` of the Dev Service from the injected config (`quarkus.topaz.authorizer.host`/`port`), not merely contains `:` |
| `TopazHealthDownTest` unreachable-host case | assert the DOWN response's data names the endpoint and the reason contains `UNAVAILABLE` |
| `DevServicesTopazPolicyDirAndImageTest` | both `quarkus.topaz.devservices.policy.directory` and `quarkus.topaz.devservices.policy.image` set → build fails with the mutual-exclusion message (read `TopazDevServicesConfig` for the exact text) |
| `DevServicesTopazEmptyDataTest` | `data-location` pointing at an empty directory → the app starts, `directory.getObject("user", "rick@the-citadel.com")` fails `NOT_FOUND` |

Dropped, with the reason recorded here: `policy.image` OCI pull and shared/Compose discovery need external resources the test suite cannot provide; post-start cleanup is covered indirectly by every Dev Service test's teardown; the IT-vs-deployment resource drift guard is obsolete since the two fixture sets intentionally differ.

- [ ] **Step 2: Run them**

Run the three affected modules with their `-Dtest` lists (`zanzibar/deployment`, `integration-tests -DskipITs`, `deployment`; `runtime` with `mvn -pl runtime test`).
Expected: green. A red test is a defect: fix it minimally in the same task and record it.

- [ ] **Step 3: Commit**

```bash
git add zanzibar integration-tests runtime deployment
git commit -m "Strengthen weak and missing tests across the client, Dev Service and zanzibar modules"
```

---

### Task 6: Build and CI housekeeping

**Files:**
- Modify: `.github/workflows/ci.yml:60-74` (`topaz-previous` job), `.github/workflows/pages.yml:51` (pin), `grpc/stubs/pom.xml:46` (Jandex plugin version), root `pom.xml` (new property `jandex-plugin.version`), `bin/generate.sh`, root `pom.xml` `test.argLine` (only if Step 2 finds the `--add-opens` source)

- [ ] **Step 1: CI**

`topaz-previous`: `mvn -B -pl deployment,security/deployment,zanzibar/deployment -am test -Dquarkus.topaz.devservices.image-name=ghcr.io/aserto-dev/topaz:0.33.21`. `pages.yml`: list the tags of `SiteNetSoft/GitHub-Actions` with `gh api repos/SiteNetSoft/GitHub-Actions/tags --jq '.[].name'`; pin to the newest tag if one exists, else to the current `master` commit SHA (`gh api repos/SiteNetSoft/GitHub-Actions/commits/master --jq .sha`) with a comment naming the date.

- [ ] **Step 2: Build hygiene**

Move the Jandex plugin version to `<jandex-plugin.version>` in the root pom's properties, referenced from `grpc/stubs/pom.xml`. `bin/generate.sh`: `set -euo pipefail`; fail if `grpc/generator/src/main/proto/aserto` is empty after export; fail if `target/generated-sources/grpc` is empty before the copy; print the pinned tags it used. For the `--add-opens` WARN lines: run `mvn -pl deployment -am verify` once, grep the output for `add-opens`; if the lines name a module (`java.base/java.lang` is typical for Byte Buddy/Mockito), add exactly those `--add-opens` to `test.argLine`; if they come from the Quarkus bootstrap itself, leave them and record that in the commit message.

- [ ] **Step 3: Verify**

Run: `JAVA_HOME=... mvn -q -T 4 -pl grpc/stubs,runtime -am package -DskipTests` (Jandex property resolves); `bash -n bin/generate.sh`; `gh workflow view "CI" --yaml | head -5` after commit (syntax). Do not run `bin/generate.sh` end to end (it re-generates committed stubs).
Expected: BUILD SUCCESS; the scripts parse.

- [ ] **Step 4: Commit**

```bash
git add .github grpc/stubs/pom.xml pom.xml bin/generate.sh
git commit -m "Run the previous-Topaz job on every deployment module, pin the pages workflow, and tidy the build"
```

---

### Task 7: Documentation and spec drift

**Files:**
- Modify: `docs/design/specs/2026-10-18-quarkus-topaz-design.md` section 5 and section 7
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/Default*.java`, `Noop*.java` (class Javadoc), `docs/modules/ROOT/pages/security.adoc` (a POJO `@TopazResource` example), `docs/modules/ROOT/pages/client.adoc` (API note: `runtime` packages are internal)
- Verify: `docs/modules/ROOT/pages/zanzibar-connector.adoc` reflects Task 1's outcome (already edited there)

- [ ] **Step 1: Amend spec section 5** to match the code, as section 4 was amended for the client: mappers are `@DefaultBean`s overridden by providing a bean of the interface (not `Instance` priority resolution); `IdentityMapper.map(SecurityIdentity, Optional<RoutingContext>)`; `policy-root` is validated at startup and not required when a custom `PolicyPathMapper` is present (Task 2); `TopazRelation(String name, String id)` with the `objectType#relation[@subjectType]` grammar and no whitespace; `ResourceMethodInfo(httpMethod, pathTemplate, pathParams)`; `@TopazCheck` on an anonymous caller is always 401; blocking dispatch via `BlockingOperationControl` and `await`, `CompletionStage` treated as blocking; `DecisionCache` keys are SHA-256 digests; denial WARN lines name the policy path and identity type, the subject is at DEBUG (Task 2); "It does not cache decisions" becomes "caches only through a user-provided `DecisionCache`".

- [ ] **Step 2: Amend spec section 7**: share/unshare-style writes need their own relation guard because `FGARelation.ANY` bypasses the check; `quarkus.topaz.zanzibar.default-user-type` and its scope; `check` maps `NOT_FOUND` to false, `remove` ignores it; the filter timeout and the client deadline (per Task 1) bound a stalled call; the user-type extraction recommendation.

- [ ] **Step 3: Javadoc and examples**: one-paragraph class Javadoc on each `Default*Mapper`, `NoopDecisionCache`, `NoopResourceMethodInfoResolver` stating what it does and how to replace it; in `security.adoc`, a short `@TopazResource` example with a POJO parameter (Jackson-serialised into the resource context) using the sample app's `Todo` type; in `client.adoc` a sentence under the API section: "Packages named `runtime` are internal."

- [ ] **Step 4: Build the docs, then the whole reactor**

Run: `JAVA_HOME=... mvn -q -T 4 -Pdocs -pl docs -am package -DskipTests`, then `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -T 4 verify -Pdocs 2>&1 | tee target/logs/pre-release-verify.log`
Expected: BUILD SUCCESS; no `Port already bound`; no new WARN categories compared with `target/logs/zanzibar-final-verify.log`.

- [ ] **Step 5: Native (after commit, alone)**

```bash
git add docs security/runtime
git commit -m "Record the security and zanzibar drift in the spec and complete the mapper Javadoc"
```
Run: `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -B -pl integration-tests -am verify -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8 2>&1 | tee target/logs/pre-release-native.log`
Expected: BUILD SUCCESS, Failsafe all green (the client's new deadline guard and the moved interceptor class are exercised natively).

---

## Out of scope (deliberately)

- The release workflow (Sonatype Central vs GitHub Packages) and `releasing.adoc`: a separate plan once 0.1.0's content is frozen.
- Enabling GitHub Pages with the "GitHub Actions" source: a repository setting only the owner can change.
- Dev UI page for enforcement decisions, decision-logs config, `DecisionCache` implementations: post-0.1.0 features, not fixes.
- Workflows triggering on `master`: already done.
- Build-time detection of `@TopazCheck` misconfiguration (missing `@TopazObjectId`) and reflection registration for nested POJO fields under `@TopazResource`: post-0.1.0; both stay documented as runtime denials / application responsibility.
- The `QuarkusUnitTest` deprecation WARN and the `SplitPackageProcessor` WARN from the shaded `quarkus-grpc-protoc-plugin`: upstream Quarkus, nothing to do here.
