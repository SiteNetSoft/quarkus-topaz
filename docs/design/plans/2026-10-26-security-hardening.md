# Security hardening and conventions (0.2.0) Implementation Plan

**Goal:** Close the enforcement gaps of 0.1.0 in `quarkus-topaz-security`. Bring the comments, the Javadoc and the
docs of every module in line with the Quarkus rules.

**Architecture:** One new internal bean, `MapperDispatch`, owns the decision of where the mappers run and the
timeout that bounds them. The four enforcement points call it. Behaviour tasks come first. The conventions pass
comes last, so it reads the final code and docs once.

**Tech Stack:** Quarkus 3.40.1, Arc, Mutiny, Vert.x, `QuarkusUnitTest` with the Topaz Dev Service, Antora.

**Spec:** `docs/design/specs/2026-10-25-security-hardening-design.md`. Read it before any task. Section numbers below
refer to it.

## Global Constraints

- Quarkus `3.40.1`, `maven.compiler.release=21`, Java 25 runs the build. No new dependency.
- GroupId `org.sitenetsoft`. The version on the branch is `0.2.0-SNAPSHOT`.
- The branch starts from `master` after the `v0.1.0` release run and the `0.2.0-SNAPSHOT` bump. It does not merge
  before that bump.
- Topaz image `ghcr.io/aserto-dev/topaz:0.33.22`. No `com.aserto:*` dependency, no `grpc-netty-shaded`.
- Tests that start containers run with rootless Podman:
  `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`.
- Every Maven command passes `-B -ntp`. Without `-ntp`, a `@QuarkusTest` fork can hang on a cold local repository.
- Fail closed. No path in this plan permits a request on an error or a timeout.
- `org.sitenetsoft.quarkus.topaz.security.runtime` stays `@Internal`. The only new public API is
  `default boolean blocking()` on `IdentityMapper`, `PolicyPathMapper`, `ResourceContextMapper` and `SubjectMapper`.
- Every task follows the rules of spec 5.7 for code, Javadoc, docs and commits. Tasks 8 and 9 only clean up the
  older text.
- Commit messages: one-sentence subject, a blank line, then a body in the same style. No tooling names in commits
  or committed files.

## Review Focus

1. A custom mapper bean that a CDI producer method provides must start without `policy-root`. Task 1 pins it with `RoutePolicyProducedMapperNoRootTest`.
2. A blocking method with a blocking-capable custom mapper must not need two worker threads per request. With a
   pool of one worker thread, the owner is still permitted. Task 3 pins it with `TopazAuthorizeOneWorkerTest`.
3. A blocking `SubjectMapper` behind the `@PermissionsAllowed` bridge must deny within the timeout and must not run
   on the event loop. Task 3 pins it with `PermissionsAllowedSlowMapperTest`.
4. A custom `DecisionCache` must keep the worker hop even when every mapper reports `blocking() == false`. Task 4 pins it with `RoutePolicyCustomCacheHopTest`.
5. A relation name with a non-breaking space must fail at the constructor, not at Topaz. Task 5 pins it with `TopazRelationTest.rejectsNonAsciiSpaces`.

---

### Task 1: Detect the custom `PolicyPathMapper` from the bean in use

Spec 5.1.

**Files:**
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/TopazSecurityRecorder.java`
- Modify: `security/deployment/src/main/java/org/sitenetsoft/quarkus/topaz/security/deployment/TopazSecurityProcessor.java`
  (the `validate` build step, `hasCustomPathMapper`, and the two `DotName` constants it alone uses)
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/RoutePolicyProducedMapperNoRootTest.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/RoutePolicyNonBeanMapperMissingRootTest.java`

**Interfaces:**
- Produces: `TopazSecurityRecorder.validate(boolean policyRootRequired)`. The one-argument form replaces the
  two-argument form.

- [ ] **Step 1: Write the two failing tests**

Copy the setup of `RoutePolicyCustomMapperNoRootTest`, without `policy-root`.

`RoutePolicyProducedMapperNoRootTest` adds a class `Producers` with
`@Produces @Singleton PolicyPathMapper mapper()` that returns a lambda answering `"todoApp.GET.todos"`. Test
`producedMapperNeedsNoPolicyRoot`: rick on `GET /todos` gets 200.

`RoutePolicyNonBeanMapperMissingRootTest` adds `public static class NotABean implements PolicyPathMapper` with no
bean-defining annotation. Copy the `assertException` of `RoutePolicyMissingRootTest`: a `ConfigurationException`
whose message contains `policy-root`. Test `startupFails` calls `fail("should not start")`.

- [ ] **Step 2: Run them and see both fail**

Run: `mvn -B -ntp -pl security/deployment -am test -Dtest='RoutePolicyProducedMapperNoRootTest,RoutePolicyNonBeanMapperMissingRootTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: the produced-mapper test fails at startup with the policy-root message. The non-bean test starts and
fails with "should not start".

- [ ] **Step 3: Move the check to the recorder**

`validate(boolean policyRootRequired)` returns early when the `PolicyPathMapper` bean's class is not
`DefaultPolicyPathMapper`. Read the bean class from the Arc container without creating the instance. The build step
passes only `annotationUsesTopazPolicy(index) || derivedPath`. Delete `hasCustomPathMapper` and any constant it
alone used. Give the recorder's constructor and both methods Javadoc in the style of spec 5.7.

- [ ] **Step 4: Run the new tests and the existing policy-root tests**

Run: `mvn -B -ntp -pl security/deployment -am test -Dtest='RoutePolicy*NoRootTest,RoutePolicy*MissingRootTest,AuthorizationPolicyMissingRootTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git commit -am "Detect a custom policy path mapper from the bean in use

The build step scanned the index for implementors of PolicyPathMapper. It
missed a mapper from a producer method and counted a class that is not a
bean. The recorder now reads the class of the bean that Arc resolves."
```

---

### Task 2: One dispatch for the mappers, used by the route policy

Spec 5.2, first half.

**Files:**
- Create: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/MapperDispatch.java`
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/DecisionEvaluator.java`
  (`evaluateRequest`, `mapperExecutor`)
- Modify: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/RoutePolicySlowMapperTest.java`

**Interfaces:**
- Produces:
  - `@Singleton final class MapperDispatch`, package-private.
  - `enum MapperDispatch.Point { ROUTE, AUTHORIZE, CHECK, BRIDGE_DECISION, BRIDGE_RELATION }`.
  - `<T> Uni<T> run(Supplier<Uni<T>> assembly, Point point, boolean callerMayBlock)`.
- Contract of `run`: when `callerMayBlock` is false, emit on the mapper executor of the current Vert.x context,
  call `assembly` there, apply `ifNoItem().after(config.timeout()).fail()`, and resume on the caller's context when
  there is one. When `callerMayBlock` is true, call `assembly` on the subscribing thread with the same timeout.
  Task 4 gives `point` its use.

- [ ] **Step 1: Rewrite `RoutePolicySlowMapperTest` to pin the timeout cause**

Drop the latch and the sleep. The mapper calls `Thread.sleep(2000)`. Set `quarkus.topaz.enforcement.timeout=500ms`.
The test asserts 403 in under 1500 ms. The log assertion requires a WARN from `DecisionEvaluator` whose message
contains both `todoApp.GET.todos` and `TimeoutException`.

- [ ] **Step 2: Run it**

Run: `mvn -B -ntp -pl security/deployment -am test -Dtest=RoutePolicySlowMapperTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS. The behaviour exists today. The test now pins the cause and needs no sleep.

- [ ] **Step 3: Create `MapperDispatch` and route `evaluateRequest` through it**

Move `mapperExecutor` and the resume-on-context step from `DecisionEvaluator` into `MapperDispatch`.
`evaluateRequest` calls `dispatch.run(() -> { resolve, map path, map identity, map resource, evaluate }, Point.ROUTE, false)`.
Keep the `recoverWithItem(deny(...))` and the unresolved-path log text in `DecisionEvaluator`.

- [ ] **Step 4: Run the security deployment tests**

Run: `mvn -B -ntp -pl security/deployment -am test`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git commit -am "Move the mapper worker hop and timeout into one dispatch bean

The route policy keeps its behaviour. The interceptors and the permission
bridge use the same bean in the next change."
```

---

### Task 3: Interceptors and the permission bridge use the dispatch

Spec 5.2, second half.

**Files:**
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/TopazAuthorizeInterceptor.java`
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/TopazCheckInterceptor.java`
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/TopazPermissionAugmentor.java`
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/InterceptorSupport.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/TopazAuthorizeSlowMapperTest.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/TopazAuthorizeOneWorkerTest.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/TopazCheckSlowMapperTest.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/PermissionsAllowedSlowMapperTest.java`

**Interfaces:**
- Consumes: `MapperDispatch.run(Supplier<Uni<T>>, Point, boolean)` from Task 2.
- Produces: `static boolean InterceptorSupport.blockingDispatch(Method method)`. True when the return type is
  neither `Uni` nor `Multi` and `BlockingOperationControl.isBlockingAllowed()` is true.

The interceptors pass `blockingDispatch(method)` as `callerMayBlock`. The bridge always passes `false`. A permission
checker cannot see how the secured method dispatches, so it takes the hop.

- [ ] **Step 1: Write the failing tests**

Each slow mapper sleeps 2000 ms, records `Thread.currentThread().getName()` in a static field, then returns. Each
test sets `quarkus.topaz.enforcement.timeout=1s` and `policy-root=todoApp`, and uses the Dev Service setup of
`TopazAuthorizeTest`.

- `TopazAuthorizeSlowMapperTest` with a slow `IdentityMapper`:
  - `reactiveDeniesOffTheEventLoop`: rick on `DELETE /service/reactive/todo-1` gets 403 in under 3 s. The recorded
    thread name does not start with `vert.x-eventloop`.
  - `blockingDeniesWithinTimeoutAndGrace`: rick on `DELETE /service/blocking/todo-1` gets 403 in under 4 s.
- `TopazAuthorizeOneWorkerTest` with an `IdentityMapper` that returns `IdentityContexts.subject(name)` at once and
  keeps the default `blocking()`. Config `quarkus.thread-pool.core-threads=1` and `quarkus.thread-pool.max-threads=1`.
  - `blockingOwnerPermittedWithOneWorker`: rick on `DELETE /service/blocking/todo-1` gets 200 with body
    `deleted todo-1`.
- `TopazCheckSlowMapperTest` with a slow `SubjectMapper`:
  - `reactiveDeniesOffTheEventLoop`: `DELETE /service/owned/todo-1` gets 403 in under 3 s, off the event loop.
  - `blockingDeniesWithinTimeoutAndGrace`: the `owned-dto` endpoint with body `{"id":"todo-1"}` gets 403 in under 4 s.
- `PermissionsAllowedSlowMapperTest` with a slow `SubjectMapper` and
  `quarkus.topaz.enforcement.permissions.enabled=true`:
  - `relationDeniesOffTheEventLoop`: rick on `DELETE /perm/todos/todo-1` gets 403 in under 3 s, off the event loop.

- [ ] **Step 2: Run them and see which fail**

Run: `mvn -B -ntp -pl security/deployment -am test -Dtest='*SlowMapperTest,TopazAuthorizeOneWorkerTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: the reactive cases record an event-loop thread and fail. The blocking cases may pass already through the
outer await. `TopazAuthorizeOneWorkerTest` passes today and must still pass after Step 3.

- [ ] **Step 3: Route the mapper calls through `MapperDispatch`**

- `TopazAuthorizeInterceptor.authorize` wraps the path derivation, the identity mapping, the resource mapping and the
  evaluation in `dispatch.run(..., Point.AUTHORIZE, blockingDispatch(method))`.
- `TopazCheckInterceptor` wraps the object-id resolution, the subject mapping and the directory check in
  `dispatch.run(..., Point.CHECK, blockingDispatch(method))`.
- `TopazPermissionAugmentor` wraps the name path in `Point.BRIDGE_DECISION` and the relation path in
  `Point.BRIDGE_RELATION`, both with `false`.
- `InterceptorSupport.failClosed` and the outer `ifNoItem` of the augmentor stay as the last guard.

- [ ] **Step 4: Run all security tests**

Run: `mvn -B -ntp -pl security/deployment -am test`
Expected: all pass, including the four new classes.

- [ ] **Step 5: Commit**

```bash
git commit -am "Run the interceptor and bridge mappers through the dispatch bean

A mapper that blocks under @TopazAuthorize, @TopazCheck or the
@PermissionsAllowed bridge is now bounded by the enforcement timeout and
never runs on the event loop. A blocking method keeps one worker thread per
request."
```

---

### Task 4: Fast path for mappers that do not block

Spec 5.3.

**Files:**
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/IdentityMapper.java`,
  `PolicyPathMapper.java`, `ResourceContextMapper.java`, `SubjectMapper.java` in the same package
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/DefaultIdentityMapper.java`,
  `DefaultPolicyPathMapper.java`, `DefaultResourceContextMapper.java`, `DefaultSubjectMapper.java`
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/MapperDispatch.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/RoutePolicyFastPathTest.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/RoutePolicyBlockingMapperHopTest.java`
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/RoutePolicyCustomCacheHopTest.java`

**Interfaces:**
- Consumes: `MapperDispatch.Point` from Task 2.
- Produces: `default boolean blocking()` returning `true` on the four mapper interfaces. The four default mappers
  return `false`.

`MapperDispatch` computes one flag per `Point` in its constructor. A point skips the hop when every component
it calls is free of blocking:

| Point | Components |
|---|---|
| `ROUTE` | resolver, path mapper, identity mapper, resource mapper, cache |
| `AUTHORIZE` | resolver, path mapper, identity mapper, resource mapper, cache |
| `CHECK` | subject mapper |
| `BRIDGE_DECISION` | identity mapper, cache |
| `BRIDGE_RELATION` | subject mapper |

A mapper is free of blocking when `blocking()` returns false. The cache is free of blocking only when it is a
`NoopDecisionCache`. The resolver is free of blocking only when it is a `NoopResourceMethodInfoResolver` or a
`RestResourceMethodInfoResolver`. The spec puts these `instanceof` checks in the recorder. Computing them once in the
singleton is the same check at the same moment and needs no recorder plumbing.

- [ ] **Step 1: Write the failing tests**

Each test registers a `ResourceContextMapper` that records the thread name and returns an empty `Struct`.

- `RoutePolicyFastPathTest`: the mapper overrides `blocking()` to return `false`. Test `defaultsRunOnTheEventLoop`:
  rick on `GET /todos` gets 200. The recorded thread name starts with `vert.x-eventloop`.
- `RoutePolicyBlockingMapperHopTest`: the mapper keeps the default. Test `blockingMapperTakesTheHop`: 200, and the
  recorded thread name does not start with `vert.x-eventloop`.
- `RoutePolicyCustomCacheHopTest`: the mapper returns `false`. The test also registers a `@Singleton`
  `DecisionCache` backed by a `ConcurrentHashMap`. Test `customCacheTakesTheHop`: 200, thread is not an event-loop thread.

- [ ] **Step 2: Run them**

Run: `mvn -B -ntp -pl security/deployment -am test -Dtest='RoutePolicy*Hop*Test,RoutePolicyFastPathTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `RoutePolicyFastPathTest` fails to compile or records a worker thread. The other two pass.

- [ ] **Step 3: Add `blocking()` and the fast path**

When the flag for `point` is set, `run` calls `assembly` on the subscribing thread with the timeout. Otherwise it
keeps the Task 2 contract. Each `blocking()` Javadoc states the contract from spec 5.3. A mapper that returns
`false` must not block and must not use `@RequestScoped` beans.

- [ ] **Step 4: Run all security tests and the API tests**

Run: `mvn -B -ntp -pl security/deployment -am test`
Expected: all pass. `InternalPackageTest` still passes.

- [ ] **Step 5: Commit**

```bash
git commit -am "Skip the worker hop when no mapper of a request blocks

Each mapper interface gets a blocking() default method that returns true.
The default mappers return false. A custom cache or resolver keeps the hop."
```

---

### Task 5: Strict grammar for `TopazRelation` names

Spec 5.4.

**Files:**
- Modify: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/TopazRelation.java`
- Test: `security/runtime/src/test/java/org/sitenetsoft/quarkus/topaz/security/TopazRelationTest.java`

**Interfaces:**
- Produces: no signature change. The constructor throws `IllegalArgumentException` for any part outside the pattern.
  The message names the part, for example `relation 'can read' in TopazRelation name 'todo#can read'`.

- [ ] **Step 1: Find the identifier rule of Topaz 0.33.22**

Topaz validates manifest names in the `aserto-dev/azm` module. Find the version in Topaz's `go.mod` at tag
`v0.33.22`, then the identifier pattern in that azm version. Write the pattern and its source file in the commit
body. If you cannot find the rule, use `[A-Za-z0-9_.-]+`.

- [ ] **Step 2: Write the failing tests**

- `rejectsNonAsciiSpaces`: `"todo#can read"` and `"todo#can read"` throw, and the message contains
  `relation`.
- `rejectsTabAndEmptyParts`: `"todo#can\tread"`, `"#owner"`, `"todo#"` and `"todo#owner@"` throw.
- `acceptsTopazNames`: `"todo#can_read@user"` and `"doc-v2#viewer"` build, with the expected `objectType()`,
  `relation()` and `subjectType()`.

- [ ] **Step 3: Run them**

Run: `mvn -B -ntp -pl security/runtime test -Dtest=TopazRelationTest`
Expected: `rejectsNonAsciiSpaces` fails.

- [ ] **Step 4: Validate each part against the pattern**

- [ ] **Step 5: Run the test and the bridge tests**

Run: `mvn -B -ntp -pl security/deployment -am test -Dtest='TopazRelationTest,PermissionsAllowed*' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: all pass.

- [ ] **Step 6: Commit**

---

### Task 6: Health timeout, test listener helpers and guide links

Spec 5.5, third bullet, and 5.6.

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/health/TopazHealthCheck.java:28`
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/devui/TopazJsonRpcService.java:27,54`
- Create: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/SilentListener.java`
- Create: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/SilentListener.java`
- Modify: the six test classes that declare `silentListener()` in those two modules
- Modify: `runtime/src/main/resources/META-INF/quarkus-extension.yaml`,
  `security/runtime/src/main/resources/META-INF/quarkus-extension.yaml`,
  `zanzibar/runtime/src/main/resources/META-INF/quarkus-extension.yaml`

**Interfaces:**
- Produces: `public static final Duration HEALTH_TIMEOUT` on `TopazHealthCheck`. `final class SilentListener` with
  `static ServerSocket open()` in each of the two test packages. The zanzibar module keeps its single copy.

- [ ] **Step 1: Make `TopazJsonRpcService` use `TopazHealthCheck.HEALTH_TIMEOUT`**

The stalled message becomes `"Topaz did not answer within " + HEALTH_TIMEOUT.toSeconds() + "s"`.

- [ ] **Step 2: Replace the six copies of `silentListener()` with `SilentListener.open()`**

- [ ] **Step 3: Set `guide: "https://sitenetsoft.org/quarkus-topaz/"` in the three descriptors**

- [ ] **Step 4: Run the client and security tests**

Run: `mvn -B -ntp -pl deployment,security/deployment -am test`
Expected: all pass, including `TopazHealthStalledTest`, `DeadlineOnStalledConnectionTest` and the Dev UI tests.

- [ ] **Step 5: Commit**

---

### Task 7: Security page and the as-built note

Spec 5.5, first two bullets. Written in the style of spec 5.7.

**Files:**
- Modify: `docs/modules/ROOT/pages/security.adoc` (section "Mapper threading")
- Modify: `docs/design/specs/2026-10-18-quarkus-topaz-design.md` (end of section 5)

- [ ] **Step 1: Rewrite "Mapper threading"**

The section covers five things:

- the single rule for all four enforcement points
- the exception for a blocking method
- the `blocking()` contract and one example of a mapper that opts in
- the table of components per enforcement point from Task 4
- a timed-out mapper keeps its worker thread until it returns

- [ ] **Step 2: Add an "As built for 0.2.0" note to the design spec**

List the items of spec 5.5, second bullet, one line each.

- [ ] **Step 3: Check the prose**

Fetch `prose-lint.java` from the diff of quarkusio/quarkus#57821 into a directory outside the repository. Run
`java prose-lint.java docs/modules/ROOT/pages/security.adoc` and fix every finding in the new section. A
passive-voice finding is a prompt to re-read, not an error.

- [ ] **Step 4: Build the docs**

Run: `mvn -B -ntp -pl docs -Pdocs package`
Expected: BUILD SUCCESS with no AsciiDoc warning.

- [ ] **Step 5: Commit**

---

### Task 8: Javadoc and comment pass on the main code

Spec 5.7, rules for comments and Javadoc, and the scope list. No code change.

**Files:**
- Modify: every hand-written file under `*/src/main/java` in `runtime`, `deployment`, `security/runtime`,
  `security/deployment`, `zanzibar/runtime`, `zanzibar/deployment`, `integration-tests` and
  `security/no-rest-tests`. `grpc/stubs` is out of scope.

- [ ] **Step 1: Measure the gap per module**

Run: `mvn -B -ntp -q -DskipTests install`, then
`mvn -B -ntp -pl runtime,deployment,security/runtime,security/deployment,zanzibar/runtime,zanzibar/deployment javadoc:javadoc -Ddoclint=all -Dshow=package`.
Record the warning count per module in the first commit body.

- [ ] **Step 2: Fix one module at a time, one commit per module**

For each module, in this order: `security/runtime`, `runtime`, `zanzibar/runtime`, then the three deployment modules,
then the two test applications.

- Add the missing Javadoc blocks. A record gets one `@param` per component.
- Turn the first sentence of each method's Javadoc into the imperative: "Evaluates…" becomes "Evaluate…".
- Delete comments that restate a statement. Keep comments that name a block or say why non-obvious code exists.
- Split sentences over about 20 words. Remove clause-joining semicolons and dashes in comments and Javadoc.

- [ ] **Step 3: Check each module before its commit**

Run the javadoc command of Step 1 with `-pl <module>`.
Expected: zero warnings for that module. Then run `mvn -B -ntp -pl <module> -am test` and expect all tests to pass.

- [ ] **Step 4: Commit per module**

Subject pattern: `Add the missing Javadoc and tidy comments in <module>`.

---

### Task 9: Prose pass on the docs and the README

Spec 5.7, rules for sentences, and the scope list. No change to documented behaviour, configuration keys or
examples.

**Files:**
- Modify: every page under `docs/modules/ROOT/pages/` and `README.md`

- [ ] **Step 1: Run the checker on every page and record the counts**

Run `java prose-lint.java` on each file. Record the counts in the commit body.

- [ ] **Step 2: Rewrite each page**

Work page by page. Fix long sentences, parentheses that hold an essential fact, clause-joining semicolons and
dashes, filler words and nominalizations. Use one term per concept across pages. Do not touch listings, tables,
attribute lines or xrefs.

- [ ] **Step 3: Recheck and build**

Run the checker again. The remaining findings must be passive voice that reads better as is, or false positives on
AsciiDoc syntax. Run `mvn -B -ntp -pl docs -Pdocs package` and expect BUILD SUCCESS.

- [ ] **Step 4: Commit**

One commit for the client pages, one for the security and zanzibar pages, one for the rest and the README.

---

### Task 10: Whole-branch verification

- [ ] **Step 1: Full build**

Run: `mvn -B -ntp verify -Pdocs`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Trace check**

Run `git diff master | grep -E '/home/|/run/user/'`.
Expected: no match. Read `git diff master --stat` and confirm that no added file is a local tool configuration
and no committed text names the tools used to write it.

- [ ] **Step 3: Push the branch and watch CI**

Expected: the build, previous-Topaz and native jobs pass.
