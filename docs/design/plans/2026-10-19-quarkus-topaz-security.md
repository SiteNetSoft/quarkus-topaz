# quarkus-topaz-security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `quarkus-topaz-security`: a named `topaz` `HttpSecurityPolicy`, the `@TopazAuthorize` and `@TopazCheck` interceptors, and an opt-in `@PermissionsAllowed` bridge, all enforcing through the existing `quarkus-topaz` client and failing closed.

**Architecture:** Two new reactor modules, `security/runtime` (`quarkus-topaz-security`) and `security/deployment` (`quarkus-topaz-security-deployment`). One `DecisionEvaluator` builds `IsRequest`s from three overridable mapper beans (identity, policy path, resource context) and interprets the response; the HTTP policy, both interceptors and the permission bridge all delegate to it. REST awareness (JAX-RS template and path parameters after matching) lives behind a `ResourceMethodInfoResolver` bean that the deployment swaps in only when `quarkus-rest` is present.

**Tech Stack:** Quarkus 3.40.1 (`quarkus-vertx-http` security SPI, `quarkus-security`, ArC interceptors, `quarkus-rest` provided), Mutiny, `quarkus-topaz` client, Dev Service Topaz 0.33.22 for tests, `quarkus-security-test-utils` for test identities.

**Spec:** `docs/design/specs/2026-10-18-quarkus-topaz-design.md`, section 5 (plus sections 2, 3, 8, 9, 11.3). The client plan `docs/design/plans/2026-10-18-quarkus-topaz-client.md` describes the API this plan consumes.

## Global Constraints

- Quarkus `3.40.1`; `quarkus.version` property; `io.quarkus:quarkus-bom` imported; nothing else version-pinned except what the BOM does not manage (`io.quarkus:quarkus-security-test-utils` is not BOM-managed and is pinned to `${quarkus.version}`, test scope only).
- GroupId `org.sitenetsoft`, version `0.1.0-SNAPSHOT`, no license headers in files (LICENSE file only).
- `maven.compiler.release=21`; build with `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`.
- Topaz image `ghcr.io/aserto-dev/topaz:0.33.22`. No dependency on any `com.aserto:*` artifact.
- `quarkus-topaz-security` depends on `quarkus-topaz`, `quarkus-vertx-http`, `quarkus-security`, `quarkus-jackson`, and `quarkus-rest` in `provided` scope. `quarkus-topaz` itself must not gain a `quarkus-security` dependency.
- Container runtime is rootless Podman: every test or build command that starts containers runs with `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`.
- Maven parallelism at most `-T 4` locally; one heavy job at a time; native builds always pass `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`; commit before any native build.
- Fail closed: a Topaz error, timeout, unresolved user, missing decision or missing policy path denies. Nothing is enforced unless the user binds the `topaz` policy or uses an annotation; no global policy.
- Package roots: public API in `org.sitenetsoft.quarkus.topaz.security`, internals in `org.sitenetsoft.quarkus.topaz.security.runtime`, build steps in `org.sitenetsoft.quarkus.topaz.security.deployment`.
- Feature name `topaz-security`. Config prefix `quarkus.topaz.enforcement`. Identity attribute key `topaz.decisions`.
- Default policy path convention: `<policy-root>.<METHOD>.<path with / replaced by . and {param} replaced by __param>`.
- Interceptor priority `Interceptor.Priority.PLATFORM_BEFORE + 210` (Quarkus's own security interceptors run at `PLATFORM_BEFORE + 150`).
- Test identities: `quarkus.http.auth.basic=true` with `TestIdentityController`/`TestIdentityProvider` from `quarkus-security-test-utils`; users `rick@the-citadel.com` (owner of `todo-1`) and `morty@the-citadel.com` exist in the directory, `summer@the-citadel.com` does not.

## Review Focus

1. A user that is authenticated in Quarkus but unknown to the Topaz directory must get 403, never 500 or 200 (Topaz answers NOT_FOUND E30017). Pinned by `RoutePolicyTest.unknownUserIsDenied` (Task 3).
2. When Topaz is unreachable or slow, every request must be denied within `quarkus.topaz.enforcement.timeout`, not hang. Pinned by `RoutePolicyTopazDownTest` (Task 3).
3. The policy bound before REST matching (`applies-to` unset) cannot see path templates: `DELETE /todos/todo-1` maps to `todoApp.DELETE.todos.todo-1`, which Topaz rejects as INVALID_ARGUMENT, so the request is denied and the WARN names the path. Pinned by `RoutePolicyBeforeMatchingTest.pathParamsNeedJaxrsMode` (Task 4).
4. A `@TopazAuthorize` with an empty `path` on a bean that is not a JAX-RS resource has no request to derive a path from and must fail the build, not deny at runtime. Pinned by `TopazAuthorizeNoPathBuildTest` (Task 5).
5. A `@TopazCheck` on an anonymous caller must yield 401, not 403 or 500. Pinned by `TopazCheckTest.anonymousIsUnauthorized` (Task 6).

---

### Task 1: Module scaffold, feature and config roots

**Files:**
- Create: `security/pom.xml` (packaging `pom`, modules `runtime`, `deployment`)
- Create: `security/runtime/pom.xml` (artifact `quarkus-topaz-security`, name `Topaz Security - Runtime`; deps per Global Constraints plus test deps `junit-jupiter`, `assertj-core`, `mockito-core`, `org.ow2.asm:asm`; the `quarkus-extension-maven-plugin` descriptor and `quarkus-extension-processor` blocks copied from `runtime/pom.xml`)
- Create: `security/deployment/pom.xml` (artifact `quarkus-topaz-security-deployment`; deps `quarkus-topaz-deployment`, `quarkus-vertx-http-deployment`, `quarkus-security-deployment`, `quarkus-jackson-deployment`, `quarkus-arc-deployment`, `quarkus-topaz-security`; test deps `quarkus-junit-internal`, `quarkus-rest-deployment`, `quarkus-rest-jackson-deployment`, `rest-assured`, `assertj-core`, `io.quarkus:quarkus-security-test-utils:${quarkus.version}`)
- Create: `security/runtime/src/main/resources/META-INF/quarkus-extension.yaml` (name `Topaz Security`, artifactId `quarkus-topaz-security`, categories `security`, status `preview`, config `quarkus.topaz.enforcement.`, same keywords and guide URL as the client yaml)
- Create: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/TopazEnforcementConfig.java`
- Create: `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/TopazEnforcementBuildTimeConfig.java`
- Create: `security/deployment/src/main/java/org/sitenetsoft/quarkus/topaz/security/deployment/TopazSecurityProcessor.java`
- Modify: `pom.xml` (root): add `<module>security</module>` after `deployment`; `docs/pom.xml`: add dependency on `quarkus-topaz-security-deployment` so the config reference includes the new keys
- Test: `security/deployment/src/test/java/org/sitenetsoft/quarkus/topaz/security/deployment/TopazSecurityFeatureTest.java`

**Interfaces:**
- Produces: `TopazEnforcementConfig` (`@ConfigMapping(prefix = "quarkus.topaz.enforcement") @ConfigRoot(phase = RUN_TIME)`): `Optional<String> policyRoot()`, `@WithDefault("allowed") String decision()`, `Optional<List<String>> additionalDecisions()`, `@WithDefault("deny") Anonymous anonymous()` with `enum Anonymous { DENY, EVALUATE }`, `@WithDefault("5s") Duration timeout()`.
- Produces: `TopazEnforcementBuildTimeConfig` (`prefix = "quarkus.topaz.enforcement"`, `BUILD_TIME`): `@WithDefault("false") boolean readBody()`, `Permissions permissions()` with `@WithDefault("false") boolean enabled()` (key `quarkus.topaz.enforcement.permissions.enabled`).
- Produces: `TopazSecurityProcessor.FEATURE = "topaz-security"`.

- [ ] **Step 1: Write the failing test**

```java
public class TopazSecurityFeatureTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.port", "1")
            .overrideConfigKey("quarkus.topaz.directory.port", "1")
            .overrideConfigKey("quarkus.topaz.plain-text", "true");

    @Inject TopazEnforcementConfig config;

    @Test
    void defaults() {
        assertThat(config.decision()).isEqualTo("allowed");
        assertThat(config.anonymous()).isEqualTo(TopazEnforcementConfig.Anonymous.DENY);
        assertThat(config.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.policyRoot()).isEmpty();
        assertThat(config.additionalDecisions()).isEmpty();
    }

    @Test
    void featureIsInstalled() {
        // the extension's own check: the fixture below records the "Installed features" log line
        assertThat(TopazSecurityProcessor.FEATURE).isEqualTo("topaz-security");
    }
}
```

The fixture adds `.setLogRecordPredicate(r -> r.getMessage().contains("Installed features"))` and `.assertLogRecords(records -> assertThat(records).anyMatch(r -> r.getMessage().contains("topaz-security")))`, which is the real feature assertion.

- [ ] **Step 2: Run it to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -q -T 4 -pl security/deployment -am test -Dtest=TopazSecurityFeatureTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (module or classes missing).

- [ ] **Step 3: Create the modules, yaml, config roots and processor** with `feature()` returning `new FeatureBuildItem(FEATURE)` and `indexRuntime()` returning `new IndexDependencyBuildItem("org.sitenetsoft", "quarkus-topaz-security")` (Quarkus reads annotation targets and constructor parameter names of `TopazRelation` from the Jandex index). Set `<parameters>true</parameters>` in the security runtime module's `maven-compiler-plugin` configuration so parameter names survive compilation. No `ExtensionSslNativeSupportBuildItem`: the client extension already registers it.

- [ ] **Step 4: Run the test to verify it passes**

Same command. Expected: PASS, and the Quarkus console shows `topaz-security` among installed features.

- [ ] **Step 5: Commit**

```bash
git add pom.xml security docs/pom.xml
git commit -m "Scaffold quarkus-topaz-security modules with enforcement config"
```

---

### Task 2: Mapper SPI and default mappers

**Files:**
- Create under `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/`: `IdentityMapper.java`, `PolicyPathMapper.java`, `ResourceContextMapper.java`, `SubjectMapper.java`, `ResourceMethodInfo.java`, `ResourceMethodInfoResolver.java`, `DecisionCache.java`
- Create under `security/runtime/src/main/java/org/sitenetsoft/quarkus/topaz/security/runtime/`: `DefaultIdentityMapper.java`, `DefaultPolicyPathMapper.java`, `DefaultResourceContextMapper.java`, `DefaultSubjectMapper.java`, `NoopResourceMethodInfoResolver.java`, `NoopDecisionCache.java`, `PolicyPaths.java`
- Modify: `TopazSecurityProcessor.java`: `AdditionalBeanBuildItem` for the six default beans (all `@Singleton @DefaultBean`)
- Test: `security/runtime/src/test/java/org/sitenetsoft/quarkus/topaz/security/runtime/PolicyPathsTest.java`, `DefaultIdentityMapperTest.java`, `DefaultResourceContextMapperTest.java`

**Interfaces:**
- Produces: `record ResourceMethodInfo(String httpMethod, String pathTemplate, Map<String, String> pathParams)` where `pathTemplate` is the class template joined with the method template, starting with `/`, e.g. `/todos/{id}`.
- Produces: `interface ResourceMethodInfoResolver { Optional<ResourceMethodInfo> resolve(RoutingContext ctx); }`; `NoopResourceMethodInfoResolver` returns `Optional.empty()`.
- Produces: `interface IdentityMapper { Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request); }`. Default: anonymous → `IdentityContexts.none()`; `identity.getCredential(TokenCredential.class) != null` → `IdentityContexts.jwt(token)`; else `IdentityContexts.subject(principalName)`.
- Produces: `interface PolicyPathMapper { String map(RoutingContext request, Optional<ResourceMethodInfo> method); }`. Default: `PolicyPaths.of(policyRoot, httpMethod, template-or-raw-path)` where the raw path is `request.normalizedPath()` with `quarkus.http.root-path` stripped (read `io.quarkus.vertx.http.runtime.VertxHttpConfig`'s root path via `HttpRootPathBuildItem`-injected value: simplest is to inject `@ConfigProperty(name = "quarkus.http.root-path", defaultValue = "/") String rootPath`).
- Produces: `final class PolicyPaths { static String of(String root, String httpMethod, String path) }`: strips leading/trailing `/`, splits on `/`, converts `{name}` and `{name:regex}` segments to `__name`, joins with `.`; empty path gives `root.METHOD`.
- Produces: `interface ResourceContextMapper { Uni<Struct> map(RoutingContext request, Optional<ResourceMethodInfo> method); }`. Default: `Structs.of(map)` where map = all `method.pathParams()` when present, then every query parameter (single value as `String`, repeated as `List<String>`; a query param never overrides a path param of the same name).
- Produces: `interface SubjectMapper { Uni<String> map(SecurityIdentity identity); }`. Default: anonymous → failure `UnauthorizedException`; principal `instanceof JsonWebToken jwt` → `jwt.getSubject()`; else `getPrincipal().getName()`. (`org.eclipse.microprofile.jwt.JsonWebToken` comes transitively via `quarkus-security`'s dependency on the MicroProfile JWT API; if the compile fails, add `org.eclipse.microprofile.jwt:microprofile-jwt-auth-api` with `provided` scope and guard the instanceof with a class-presence check.)
- Produces: `interface DecisionCache { Optional<Boolean> get(String key); void put(String key, boolean allowed); }`; `NoopDecisionCache` always empty. Key format is `identity|policyPath|decision|resourceJson`, built by Task 3.

- [ ] **Step 1: Write the failing tests**

```java
class PolicyPathsTest {
    @ParameterizedTest
    @CsvSource({
        "todoApp, GET, /todos, todoApp.GET.todos",
        "todoApp, DELETE, /todos/{id}, todoApp.DELETE.todos.__id",
        "todoApp, PUT, /todos/{id:[0-9]+}/items/{itemId}, todoApp.PUT.todos.__id.items.__itemId",
        "todoApp, GET, /, todoApp.GET",
        "todoApp, GET, todos/, todoApp.GET.todos" })
    void convention(String root, String method, String path, String expected) {
        assertThat(PolicyPaths.of(root, method, path)).isEqualTo(expected);
    }
}
```

```java
class DefaultIdentityMapperTest {
    @Test void anonymousIsNone()  // QuarkusSecurityIdentity.builder().setAnonymous(true).build() → type IDENTITY_TYPE_NONE
    @Test void tokenCredentialIsJwt()  // addCredential(new TokenCredential("x.y.z", "bearer")) → IDENTITY_TYPE_JWT, identity "x.y.z"
    @Test void principalIsSub()  // setPrincipal(() -> "rick@the-citadel.com") → IDENTITY_TYPE_SUB, identity "rick@the-citadel.com"
}
```

```java
class DefaultResourceContextMapperTest {  // Mockito-mocked RoutingContext + HttpServerRequest with MultiMap params
    @Test void pathParamsAndQueryParams()  // method pathParams {id=todo-1}, query ?q=a&tag=x&tag=y → {"id":"todo-1","q":"a","tag":["x","y"]}
    @Test void pathParamWinsOverQueryParam()  // path {id=todo-1}, query ?id=evil → id stays "todo-1"
    @Test void noMethodInfoGivesQueryOnly()
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -q -T 4 -pl security/runtime -am test -Dtest='PolicyPathsTest,DefaultIdentityMapperTest,DefaultResourceContextMapperTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (classes missing).

- [ ] **Step 3: Implement the interfaces, defaults and `PolicyPaths`**, register the defaults in the processor.

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add security
git commit -m "Add Topaz enforcement mapper SPI with default implementations"
```

---

### Task 3: DecisionEvaluator and the `topaz` HttpSecurityPolicy

**Files:**
- Create: `security/runtime/.../security/runtime/DecisionEvaluator.java`, `TopazHttpSecurityPolicy.java`, `TopazSecurityRecorder.java`
- Modify: `TopazSecurityProcessor.java`: `AdditionalBeanBuildItem.unremovableOf(TopazHttpSecurityPolicy.class)`, `UnremovableBeanBuildItem.beanTypes(HttpSecurityPolicy.class)`, and a `@Record(RUNTIME_INIT)` step calling `recorder.validate(boolean annotationUsesTopazPolicy)` after `VertxBuildItem` (the boolean comes from a Jandex scan for `io.quarkus.vertx.http.security.AuthorizationPolicy` with `name == "topaz"`)
- Test resources: copy `deployment/src/test/resources/topaz/` to `security/deployment/src/test/resources/topaz/` (same five files), then extend the copied `policies/todo.rego` (package `todoApp.GET.todos`) with a second decision: `default visible = false` and `visible if input.user.id == "rick@the-citadel.com"`
- Test: `security/deployment/src/test/java/.../deployment/TodoTestResource.java` (`@Path("/todos")`: `GET` list returning the `topaz.decisions` attribute of the injected `SecurityIdentity` as a JSON object, or `{}` when absent; `DELETE {id}` returning 204), `RoutePolicyTest.java`, `RoutePolicyAnonymousEvaluateTest.java`, `RoutePolicyTopazDownTest.java`, `RoutePolicyMissingRootTest.java`, `RoutePolicyDecisionsAttributeTest.java`, and a shared `TestIdentities.java` helper calling `TestIdentityController.resetRoles().add("rick@the-citadel.com", "rick").add("morty@the-citadel.com", "morty").add("summer@the-citadel.com", "summer")`

**Interfaces:**
- Consumes: `AuthorizerClient.is(IdentityContext, String policyPath, List<String> decisions, Struct resource) → Uni<IsResponse>`; `IsResponse.getDecisionsList()` of `Decision { getDecision(), getIs() }`; `TopazEnforcementConfig`; mappers from Task 2.
- Produces: `final class DecisionEvaluator` (`@Singleton`): `record Outcome(boolean permitted, Map<String, Boolean> decisions)`; `Uni<Outcome> evaluate(IdentityContext identity, String policyPath, Struct resource)` asks for `[decision] + additionalDecisions`, applies `.ifNoItem().after(config.timeout()).fail()`, consults `DecisionCache` first (key as in Task 2) and stores on success, maps the response to `Outcome(is of config.decision(), map of all returned decisions)`, and on any failure logs WARN `"Topaz denied %s for %s: %s"` (policyPath, identity type+id, cause) and returns `Outcome(false, Map.of())`. Also `Uni<Outcome> evaluateRequest(SecurityIdentity, RoutingContext)` that runs the three mappers (resolver → policy path → resource context) and calls `evaluate`.
- Produces: `TopazHttpSecurityPolicy implements HttpSecurityPolicy`, `name() == "topaz"`, `@Singleton`: anonymous identity with `anonymous=deny` → `CheckResult.DENY` (Quarkus turns a denied anonymous request into 401); otherwise `evaluateRequest`; permitted with non-empty `additional-decisions` → `new CheckResult(true, QuarkusSecurityIdentity.builder(identity).addAttribute("topaz.decisions", outcome.decisions()).build())`; permitted otherwise → `CheckResult.PERMIT`; denied → `CheckResult.DENY`.
- Produces: `TopazSecurityRecorder.validate(boolean policyRootRequired)`: throws `io.quarkus.runtime.configuration.ConfigurationException("quarkus.topaz.enforcement.policy-root must be set when the 'topaz' HTTP security policy or a derived @TopazAuthorize path is used")` when `policyRoot` is empty and either the flag is true or any config property matching `quarkus.http.auth.permission.*.policy` has value `topaz`. In this task the flag is "an `@AuthorizationPolicy(name = "topaz")` annotation exists"; Task 5 extends it with "a `@TopazAuthorize` with empty `path` exists".

- [ ] **Step 1: Write the failing tests** (each class uses `QuarkusUnitTest` with `.withApplicationRoot(jar -> jar.addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class, TestIdentityController.class).addAsResource("topaz", "topaz"))`, config `quarkus.http.auth.basic=true`, the three Dev Service keys from `integration-tests/src/main/resources/application.properties`, `quarkus.topaz.enforcement.policy-root=todoApp`, `quarkus.http.auth.permission.todos.paths=/todos*`, `quarkus.http.auth.permission.todos.policy=topaz`; `@BeforeAll` calls `TestIdentities.register()`)

```java
public class RoutePolicyTest {
    @Test void knownUserIsPermitted()   { given().auth().preemptive().basic("rick@the-citadel.com", "rick").get("/todos").then().statusCode(200); }
    @Test void unknownUserIsDenied()    { given().auth().preemptive().basic("summer@the-citadel.com", "summer").get("/todos").then().statusCode(403); }
    @Test void anonymousIsUnauthorized(){ given().get("/todos").then().statusCode(401); }
}
public class RoutePolicyAnonymousEvaluateTest {   // + quarkus.topaz.enforcement.anonymous=evaluate
    @Test void anonymousIsEvaluatedAndDenied() { given().get("/todos").then().statusCode(401); }  // NONE identity → allowed=false → DENY → 401 for anonymous
    @Test void knownUserStillPermitted() { ... 200 }
}
public class RoutePolicyTopazDownTest {   // devservices.enabled=false, authorizer.port=1, directory.port=1, plain-text=true, enforcement.timeout=2s
    @Test void deniedWithinTimeout() { long t0 = System.nanoTime(); ... rick GET /todos → 403; elapsed < 5s }
}
public class RoutePolicyMissingRootTest {   // no policy-root; .assertException(t -> assertThat(t).isInstanceOf(ConfigurationException.class).hasMessageContaining("policy-root"))
    @Test void startupFails() { fail("should not start"); }
}
public class RoutePolicyDecisionsAttributeTest {   // + quarkus.topaz.enforcement.additional-decisions=visible
    @Test void rickSeesVisibleTrue()  { rick GET /todos → 200, body "visible" == true, "allowed" == true }
    @Test void mortySeesVisibleFalse(){ morty GET /todos → 200, body "visible" == false }
}
```

`RoutePolicyTest.knownUserIsPermitted` additionally asserts that the body is `{}`: without `additional-decisions` no attribute is set.

- [ ] **Step 2: Run them to verify they fail**

Run: `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -q -T 4 -pl security/deployment -am test -Dtest='RoutePolicy*' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (no `topaz` policy: Quarkus fails startup with "unknown policy topaz", or 403s where 200 is expected).

- [ ] **Step 3: Implement `DecisionEvaluator`, `TopazHttpSecurityPolicy`, the recorder validation and the processor wiring.**

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: PASS; the WARN lines for `summer@the-citadel.com` and for Topaz down name the policy path `todoApp.GET.todos`.

- [ ] **Step 5: Commit**

```bash
git add security
git commit -m "Add the named topaz HttpSecurityPolicy backed by a shared decision evaluator"
```

---

### Task 4: REST awareness: templates and path parameters after matching

**Files:**
- Create: `security/runtime/.../security/runtime/rest/RestResourceMethodInfoResolver.java` (`@Singleton`, references `org.jboss.resteasy.reactive.server.core.CurrentRequestManager` and `ResteasyReactiveRequestContext`; compiled against `quarkus-rest` provided scope)
- Modify: `TopazSecurityProcessor.java`: when `capabilities.isPresent(Capability.REST)` produce `AdditionalBeanBuildItem` for `RestResourceMethodInfoResolver` (it is a non-default bean, so it wins over the `@DefaultBean` no-op); produce `new RequireBodyHandlerBuildItem()` when `buildTimeConfig.readBody()` is true
- Test: `security/deployment/src/test/java/.../deployment/RoutePolicyJaxrsTest.java`, `AuthorizationPolicyAnnotationTest.java`, `RoutePolicyBeforeMatchingTest.java`

**Interfaces:**
- Consumes: `ResteasyReactiveRequestContext ctx = CurrentRequestManager.get()`; `ctx.getTarget()` (`RuntimeResource`): `getHttpMethod()`, `getClassPath().template`, `getPath().template`, `getPathParameterIndexes()`; `ctx.getPathParameter(name, false)`.
- Produces: `RestResourceMethodInfoResolver.resolve(RoutingContext)` → `Optional.empty()` when `CurrentRequestManager.get()` or its target is null, else `ResourceMethodInfo(httpMethod, joinTemplates(classPath, methodPath), pathParams)`; `joinTemplates` normalises double slashes and a missing leading `/`.

- [ ] **Step 1: Write the failing tests** (same fixture as Task 3; `TodoTestResource` already has `DELETE /todos/{id}`)

```java
public class RoutePolicyJaxrsTest {   // permission todos: paths=/todos/*, policy=topaz, applies-to=jaxrs
    @Test void ownerCanDelete()    { rick DELETE /todos/todo-1 → 204 }   // policy todoApp.DELETE.todos.__id checks owner via resource.id
    @Test void nonOwnerIsDenied()  { morty DELETE /todos/todo-1 → 403 }
}
public class AuthorizationPolicyAnnotationTest {   // no quarkus.http.auth.permission.*; resource class annotated @AuthorizationPolicy(name = "topaz") via a test subclass AnnotatedTodoResource
    @Test void ownerCanDelete()    { rick DELETE /todos/todo-1 → 204 }
    @Test void nonOwnerIsDenied()  { morty DELETE /todos/todo-1 → 403 }
    @Test void listIsPermitted()   { rick GET /todos → 200 }
}
public class RoutePolicyBeforeMatchingTest {   // paths=/todos/*, policy=topaz, applies-to unset
    @Test void pathParamsNeedJaxrsMode() { rick DELETE /todos/todo-1 → 403; log contains "todoApp.DELETE.todos.todo-1" }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: the Task 3 command with `-Dtest='RoutePolicyJaxrsTest,AuthorizationPolicyAnnotationTest,RoutePolicyBeforeMatchingTest'`
Expected: `RoutePolicyJaxrsTest` and `AuthorizationPolicyAnnotationTest` FAIL with 403 where 204 is expected (no template, path mapped literally); `RoutePolicyBeforeMatchingTest` may already pass.

- [ ] **Step 3: Implement the resolver and the processor wiring.**

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add security
git commit -m "Resolve JAX-RS templates and path parameters for Topaz policy paths"
```

---

### Task 5: `@TopazAuthorize` and `@TopazResource`

**Files:**
- Create under `security/runtime/.../security/`: `TopazAuthorize.java` (`@InterceptorBinding @Inherited @Retention(RUNTIME) @Target({TYPE, METHOD})`, members all `@Nonbinding`: `String path() default ""`, `String decision() default ""` (empty means the configured decision), `String[] decisions() default {}`), `TopazResource.java` (`@Retention(RUNTIME) @Target(PARAMETER)`, `String value()`)
- Create: `security/runtime/.../security/runtime/TopazAuthorizeInterceptor.java` (`@Interceptor @TopazAuthorize @Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)`), `InterceptorSupport.java` (shared: current identity via `CurrentIdentityAssociation.getDeferredIdentity()`, current request via `CurrentVertxRequest.getCurrent()` as `Optional<RoutingContext>`, reactive/blocking dispatch, exception mapping)
- Modify: `TopazSecurityProcessor.java`: `AdditionalBeanBuildItem` for the interceptor; a build step that fails with `DeploymentException` when a `@TopazAuthorize` with empty `path` sits on a method or class whose declaring class has no `jakarta.ws.rs.Path` annotation; `ReflectiveClassBuildItem` (methods + fields) for every parameter type annotated `@TopazResource` that is not a JDK type
- Test: `security/deployment/src/test/java/.../deployment/TopazAuthorizeTest.java`, `TopazAuthorizeNoPathBuildTest.java`, test bean `SecuredService.java`

**Interfaces:**
- Consumes: `DecisionEvaluator.evaluate(...)`, `IdentityMapper`, `PolicyPathMapper`, `ResourceContextMapper`, `ResourceMethodInfoResolver`, `ObjectMapper` (quarkus-jackson).
- Produces: `InterceptorSupport`: `static Object dispatch(InvocationContext ctx, Uni<Void> check, Duration timeout)`: if the method's return type is `Uni` → `check.chain(() -> (Uni<?>) ctx.proceed())`; `Multi` → `Multi.createFrom().deferred(...)` after the check; otherwise, when `BlockingOperationControl.isBlockingAllowed()` → `check.await().atMost(timeout.plusSeconds(1))` then `ctx.proceed()`; else throw `IllegalStateException("@TopazAuthorize/@TopazCheck on a blocking method cannot run on the event loop; return Uni or run on a worker thread")`. The `check` Uni fails with `UnauthorizedException` for anonymous callers (when `anonymous=deny`) and `ForbiddenException` on deny.
- Produces: resource context for `@TopazAuthorize` = `ResourceContextMapper` output (empty struct outside HTTP) overlaid with each `@TopazResource("key")` parameter value converted with `Structs.value(objectMapper.convertValue(arg, Object.class))` (strings, numbers, booleans, maps, lists pass through; POJOs become maps).
- Produces: policy path = `path()` when non-empty, else `PolicyPathMapper.map(request, resolver.resolve(request))`.

- [ ] **Step 1: Write the failing tests**

```java
@ApplicationScoped
public class SecuredService {
    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public Uni<String> deleteReactive(@TopazResource("id") String id) { return Uni.createFrom().item("deleted " + id); }
    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public String deleteBlocking(@TopazResource("id") String id) { return "deleted " + id; }
    @TopazAuthorize(path = "todoApp.GET.todos", decision = "allowed")
    public String list() { return "ok"; }
}
// TodoTestResource gains endpoints that call the service: GET /service/list, DELETE /service/reactive/{id}, DELETE /service/blocking/{id} (blocking method annotated @Blocking)
public class TopazAuthorizeTest {   // no HTTP policy binding; basic auth; Dev Service
    @Test void reactiveOwnerPermitted()  { rick DELETE /service/reactive/todo-1 → 200 "deleted todo-1" }
    @Test void reactiveNonOwnerForbidden(){ morty DELETE /service/reactive/todo-1 → 403 }
    @Test void blockingOwnerPermitted()  { rick DELETE /service/blocking/todo-1 → 200 }
    @Test void blockingNonOwnerForbidden(){ morty DELETE /service/blocking/todo-1 → 403 }
    @Test void anonymousUnauthorized()   { GET /service/list → 401 }
    @Test void unknownUserForbidden()    { summer GET /service/list → 403 }
}
public class TopazAuthorizeNoPathBuildTest {   // bean NotAResource with @TopazAuthorize (no path) on a method; .assertException(t -> assertThat(t).hasMessageContaining("path"))
    @Test void buildFails() { fail(); }
}
```

`TopazAuthorizeTest` also covers the derived path: `TodoTestResource` (class path `/todos`) gains `@GET @Path("/derived") @TopazAuthorize public String derived()` with no `path`, so the derived policy path is `todoApp.GET.todos.derived`. The test policies gain `derived.rego` (package `todoApp.GET.todos.derived`, `default allowed = false`, `allowed if input.user.id != ""`), and `derivedPathFromRequest()` asserts rick GET `/todos/derived` → 200 and summer → 403. The deployment step that computes the policy-root flag for `TopazSecurityRecorder.validate` (Task 3) now also sets it when any `@TopazAuthorize` has an empty `path`.

- [ ] **Step 2: Run them to verify they fail**

Run: Task 3 command with `-Dtest='TopazAuthorize*'`. Expected: FAIL (annotation missing).

- [ ] **Step 3: Implement the annotations, interceptor, support class and build steps.**

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add security
git commit -m "Add @TopazAuthorize interceptor with @TopazResource context"
```

---

### Task 6: `@TopazCheck` and `@TopazObjectId`

**Files:**
- Create under `security/runtime/.../security/`: `TopazCheck.java` (`@InterceptorBinding`, `@Nonbinding` members `String objectType()`, `String relation()`, `String subjectType() default "user"`, `String objectId() default ""`), `TopazObjectId.java` (`@Retention(RUNTIME) @Target({PARAMETER, FIELD, METHOD})`)
- Create: `security/runtime/.../security/runtime/TopazCheckInterceptor.java` (`@Interceptor @TopazCheck @Priority(PLATFORM_BEFORE + 210)`), `ObjectIds.java` (resolves the object id: annotation constant, else the `@TopazObjectId` parameter, else the first parameter with a `@TopazObjectId` field or getter, read reflectively; none → `IllegalStateException` naming the method)
- Modify: `TopazSecurityProcessor.java`: `AdditionalBeanBuildItem` for the interceptor; `ReflectiveClassBuildItem` (fields + methods) for classes declaring `@TopazObjectId` on a field or method
- Test: `security/deployment/src/test/java/.../deployment/TopazCheckTest.java`, test bean additions to `SecuredService`

**Interfaces:**
- Consumes: `DirectoryClient.check(objectType, objectId, relation, subjectType, subjectId) → Uni<Boolean>`; `SubjectMapper`; `InterceptorSupport.dispatch`.
- Produces: `TopazCheckInterceptor`: anonymous → `UnauthorizedException`; `check(...)` false, error or timeout (`ifNoItem().after(config.timeout())`) → WARN and `ForbiddenException`.

- [ ] **Step 1: Write the failing tests**

```java
// SecuredService additions
@TopazCheck(objectType = "todo", relation = "owner")
public Uni<String> deleteOwned(@TopazObjectId String id) { ... }
@TopazCheck(objectType = "todo", relation = "owner")
public String deleteOwnedDto(TodoRef ref) { ... }      // record TodoRef(@TopazObjectId String id, String note)  → annotate the accessor: public record with @TopazObjectId on the component is propagated to the accessor method
@TopazCheck(objectType = "todo", relation = "owner", objectId = "todo-1")
public String deleteFixed() { ... }

public class TopazCheckTest {
    @Test void ownerPermitted()        { rick DELETE /service/owned/todo-1 → 200 }
    @Test void nonOwnerForbidden()     { morty DELETE /service/owned/todo-1 → 403 }
    @Test void dtoObjectIdPermitted()  { rick POST /service/owned-dto {"id":"todo-1","note":"x"} → 200 }
    @Test void fixedObjectId()         { rick DELETE /service/owned-fixed → 200; morty → 403 }
    @Test void anonymousIsUnauthorized(){ DELETE /service/owned/todo-1 → 401 }
    @Test void unknownSubjectForbidden(){ summer DELETE /service/owned/todo-1 → 403 }   // check returns false for an unknown subject; no error
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: Task 3 command with `-Dtest=TopazCheckTest`. Expected: FAIL.

- [ ] **Step 3: Implement the annotations, interceptor, `ObjectIds` and build steps.**

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add security
git commit -m "Add @TopazCheck interceptor with @TopazObjectId resolution"
```

---

### Task 7: `@PermissionsAllowed` bridge (opt-in)

**Files:**
- Create: `security/runtime/.../security/TopazRelation.java` (`extends java.security.Permission`; single constructor `TopazRelation(String name, String id)`; `name` is `objectType#relation` or `objectType#relation@subjectType`, parsed into `objectType()`, `relation()`, `subjectType()` (default `user`); `objectId()` returns `id`; `implies` returns `false`, `equals/hashCode/getActions` implemented)
- Create: `security/runtime/.../security/runtime/TopazPermissionAugmentor.java` (`@ApplicationScoped`, `implements SecurityIdentityAugmentor`, `priority() == 0`)
- Modify: `TopazSecurityProcessor.java`: `AdditionalBeanBuildItem` for the augmentor only when `buildTimeConfig.permissions().enabled()` is true
- Test: `security/deployment/src/test/java/.../deployment/PermissionsAllowedBridgeTest.java`, `PermissionsAllowedDisabledTest.java`

**Interfaces:**
- Consumes: `QuarkusSecurityIdentity.builder(identity).addPermissionChecker(Function<Permission, Uni<Boolean>>)`; `DecisionEvaluator.evaluate`; `DirectoryClient.check`; `IdentityMapper.map(identity, Optional.empty())`; `SubjectMapper`.
- Produces: augmentor skips anonymous identities; for a `TopazRelation` permission → `directory.check(objectType, objectId, relation, subjectType, subject)`; for any other permission → `evaluator.evaluate(identityContext, permission.getName(), Struct.getDefaultInstance()).map(Outcome::permitted)`; failures map to `false` (the evaluator already logs; the relation branch logs its own WARN).

- [ ] **Step 1: Write the failing tests**

```java
// test resource PermissionsResource (@Path("/perm"))
@GET @Path("/list") @PermissionsAllowed("todoApp.GET.todos") public String list()
@DELETE @Path("/todos/{id}") @PermissionsAllowed(value = "todo#owner", permission = TopazRelation.class, params = "id") public String delete(@PathParam("id") String id)

public class PermissionsAllowedBridgeTest {   // quarkus.topaz.enforcement.permissions.enabled=true, policy-root=todoApp
    @Test void policyPermission()        { rick GET /perm/list → 200; summer → 403 }
    @Test void relationPermissionOwner() { rick DELETE /perm/todos/todo-1 → 200 }
    @Test void relationPermissionDenied(){ morty DELETE /perm/todos/todo-1 → 403 }
}
public class PermissionsAllowedDisabledTest {   // enabled unset
    @Test void noRemoteCallsWithoutOptIn() { rick GET /perm/list → 403 }   // identity has no permissions, bridge absent
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: Task 3 command with `-Dtest='PermissionsAllowed*'`. Expected: `PermissionsAllowedBridgeTest` FAIL (403 everywhere).

- [ ] **Step 3: Implement `TopazRelation`, the augmentor and the conditional registration.**

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add security
git commit -m "Bridge @PermissionsAllowed to Topaz policy and relation checks (opt-in)"
```

---

### Task 8: Integration tests, JVM and native

**Files:**
- Modify: `integration-tests/pom.xml`: add `quarkus-topaz-security`, `quarkus-topaz-security-deployment` (same `pom`-type, `test` scope pattern with exclusions as the existing deployment dependency), `quarkus-elytron-security-properties-file`
- Modify: `integration-tests/src/main/resources/application.properties`: `quarkus.http.auth.basic=true`, embedded users `quarkus.security.users.embedded.enabled=true`, `.plain-text=true`, `.users."rick@the-citadel.com"=rick`, `.users."morty@the-citadel.com"=morty`, `.users."summer@the-citadel.com"=summer`, `quarkus.topaz.enforcement.policy-root=todoApp`, `quarkus.http.auth.permission.secured.paths=/secured/*`, `.policy=topaz`, `.applies-to=jaxrs`
- Create: `integration-tests/src/main/java/.../it/SecuredTodoResource.java` (`@Path("/secured/todos")`: `GET` list; `DELETE {id}` guarded by the route policy; plus `@Path("/annotated/todos")` class `AnnotatedTodoResource` with `DELETE {id}` using `@TopazCheck(objectType="todo", relation="owner")` on `@TopazObjectId @PathParam("id")` and `GET` using `@TopazAuthorize(path = "todoApp.GET.todos")`)
- Create: `integration-tests/src/test/java/.../it/SecuredTodoResourceTest.java`, `SecuredTodoResourceIT.java`

**Interfaces:**
- Consumes: everything above; the existing `TodoResource` stays unprotected.

- [ ] **Step 1: Write the failing tests**

```java
@QuarkusTest
public class SecuredTodoResourceTest {
    @Test void routePolicyOwnerDeletes()   { rick DELETE /secured/todos/todo-1 → 204 }
    @Test void routePolicyNonOwnerDenied() { morty DELETE /secured/todos/todo-1 → 403 }
    @Test void routePolicyUnknownUser()    { summer GET /secured/todos → 403 }
    @Test void routePolicyAnonymous()      { GET /secured/todos → 401 }
    @Test void annotatedCheckOwner()       { rick DELETE /annotated/todos/todo-1 → 204 }
    @Test void annotatedCheckNonOwner()    { morty DELETE /annotated/todos/todo-1 → 403 }
    @Test void annotatedAuthorizeList()    { rick GET /annotated/todos → 200 }
}
@QuarkusIntegrationTest
public class SecuredTodoResourceIT extends SecuredTodoResourceTest {}
```

- [ ] **Step 2: Run the JVM tests to verify they fail**

Run: `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -T 4 -pl integration-tests -am verify -Dtest=SecuredTodoResourceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (resources missing).

- [ ] **Step 3: Add the resources, config and dependencies.**

- [ ] **Step 4: Run the JVM tests to verify they pass**

Run: the same command without `-Dtest` (the whole module). Expected: BUILD SUCCESS, `TodoResourceTest` 6/6 and `SecuredTodoResourceTest` 7/7.

- [ ] **Step 5: Commit, then run the single native build**

```bash
git add integration-tests
git commit -m "Add secured todo endpoints to the integration tests"
```

Run: `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -pl integration-tests verify -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=podman -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8` (after `mvn -T 4 -pl security/deployment -am install -DskipTests` so the native run sees fresh jars)
Expected: BUILD SUCCESS with `SecuredTodoResourceIT` 7/7 and `TodoResourceIT` 6/6. A reflection failure in `@TopazObjectId` resolution or Jackson conversion is fixed in the deployment processor (`ReflectiveClassBuildItem`) and committed before a second, final native run.

---

### Task 9: Documentation and CI

**Files:**
- Create: `docs/modules/ROOT/pages/security.adoc`: installing the artifact; route-level enforcement (both bindings, `applies-to=jaxrs` and why), the policy-path convention table (method/path → path, including the before-matching limitation), identity mapping table (anonymous/SUB/JWT, "Topaz does not validate the JWT", "users must exist in the directory"), `additional-decisions` and the `topaz.decisions` attribute, `@TopazAuthorize`/`@TopazResource`, `@TopazCheck`/`@TopazObjectId`, the `@PermissionsAllowed` bridge with `TopazRelation`, overriding mappers (bean of the interface wins over the `@DefaultBean`), `DecisionCache`, and every `quarkus.topaz.enforcement.*` key with a one-line meaning (the full reference is generated)
- Modify: `docs/modules/ROOT/pages/nav.adoc` (add `security.adoc` after `client.adoc`), `docs/modules/ROOT/pages/index.adoc` (artifact table gains `quarkus-topaz-security`), `README.md` (one paragraph and the artifact snippet)
- Modify: `.github/workflows/ci.yml`: no job changes; the `build` job already runs `mvn -B -T 4 verify`, and the native job already builds `integration-tests`

- [ ] **Step 1: Write the page and the edits.**

- [ ] **Step 2: Build the docs**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -q -T 4 -Pdocs -pl docs -am package -DskipTests`
Expected: `docs/target/generated-docs/security.html` exists; `grep -c "quarkus.topaz.enforcement.policy-root" docs/target/generated-docs/configuration.html` ≥ 1; every key named in `security.adoc` appears in the generated include.

- [ ] **Step 3: Full build**

Run: `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -T 4 verify -Pdocs`
Expected: BUILD SUCCESS, no new WARN lines compared with the client branch's final build (`relocated`, `argLine`, `java.util.logging.manager` must stay at zero).

- [ ] **Step 4: Commit**

```bash
git add docs README.md
git commit -m "Document quarkus-topaz-security"
```

---

## Follow-up plan (not part of this document)

- `quarkus-topaz-zanzibar` (spec section 7), consuming `DirectoryClient.check/setRelation/deleteRelation`.
