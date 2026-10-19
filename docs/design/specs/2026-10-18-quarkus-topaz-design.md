# Quarkus Topaz Design

**Status:** approved design, pre-implementation (approved 2026-10-18; bytecode release 21 confirmed)
**Date:** 2026-10-18
**Deliverable:** full Quarkus support for [Topaz](https://www.topaz.sh) (aserto-dev/topaz), shipped as a
family of Quarkus extensions under `org.sitenetsoft`, built with Maven. Three consumable extensions:
`quarkus-topaz` (client, Dev Services, health, tracing, Dev UI), `quarkus-topaz-security`
(request and method enforcement) and `quarkus-topaz-zanzibar` (optional connector for
`io.quarkiverse.zanzibar:quarkus-zanzibar`).

## 1. Purpose and intent

Topaz is a self-hosted authorizer that bundles two engines in one process: a **Directory**
(Zanzibar-style objects and relations, gRPC services Reader, Writer, Model, Importer, Exporter) and an
**Authorizer** (embedded Open Policy Agent evaluating Rego policies, gRPC service with `Is`,
`DecisionTree`, `Query`, `Compile`, `ListPolicies`, `GetPolicy`, `Info`). Policies can call the
directory, so one `Is` call can combine role, attribute and relationship checks.

There is no Quarkus integration for Topaz today. The only Java material is Aserto's own SDK
(`com.aserto:aserto-java` 0.31.4, November 2024, unshaded grpc-netty, protobuf 3.25) and a Spring Boot
starter from October 2024, both dormant since Aserto shut down its SaaS in May 2025. Topaz itself is
active (v0.33.22, September 2026, monthly patches). Quarkus developers who want Topaz have to hand-roll
a gRPC client, run the container by hand and write their own filter.

**Intent:** a Quarkus developer adds one dependency, gets a Topaz container in dev and test with their
policies and directory manifest loaded, injects a typed client, and protects endpoints with either an
annotation or a line of configuration. Native image works. Nothing about Topaz is hidden: every gRPC
service is reachable through the client, and convenience methods sit on top rather than in place of them.

### Goals

- Complete coverage of the Topaz gRPC surface, Authorizer v2 and Directory v3, through Mutiny stubs
  with a blocking facade.
- Enforcement in the Topaz idiom (policy path, identity context, resource context) and in the
  relationship idiom (object, relation, subject), at the HTTP route level and at the method level.
- A Dev Service good enough that the integration tests of this project, and of its consumers, need
  no manual Topaz setup.
- Idiomatic Quarkus: `@ConfigMapping` config, TLS registry, SmallRye Health, OpenTelemetry, Dev UI,
  native image, extension metadata, Antora docs.
- Consumers choose their layers: client only; client plus security; client plus quarkus-zanzibar.

### Non-goals

- Supporting Directory v2 (legacy, Topaz removed `check_permission`/`check_relation` in v0.33.15).
- Wrapping or depending on `com.aserto:*` artifacts.
- A Rego authoring toolchain (policy CLI, bundle building). Dev Services consume policies the user
  already has, as a Rego directory or an OCI policy image.
- Management of Topaz itself (upgrades, decision log shipping, edge sync).
- Quarkiverse onboarding in this iteration. The layout keeps that door open (Maven, standard module
  split), but groupId stays `org.sitenetsoft`.

### Anti-goals (what would make this a failure even if it works)

- Forcing every user through `quarkus-zanzibar`. Topaz's Authorizer is the reason to pick Topaz; the
  Zanzibar abstraction cannot express it.
- Pulling a second gRPC/Netty stack into the application. Everything rides on `quarkus-grpc`.
- An enforcement layer that silently permits when Topaz is unreachable or the user is unknown.
  Fail closed, always, with a clear log line.
- Generated code that only compiles with a Docker-based tool on the developer's machine.

## 2. Constraints

- Quarkus 3.40.1, matching quarkus-tus and the current Quarkiverse extensions. The Quarkus 4.0 line
  removes grpc-netty from `quarkus-grpc` and the old Dev Services API; the design avoids both so the
  move is small.
- Maven, `org.sitenetsoft` groupId, Apache 2.0.
- JDK 25 toolchain for the build; **bytecode release 21** (my call, see §10) so the artifacts are
  usable from Quarkus 3.x applications on Java 21.
- Protos pinned to published tags: `pb-authorizer` v0.24.0 and `pb-directory` v0.34.1 (buf modules
  `buf.build/aserto-dev/authorizer` and `buf.build/aserto-dev/directory`). Topaz image pinned to
  `ghcr.io/aserto-dev/topaz:0.33.22`. Both bump by PR, never floating.
- Host build rules from `~/.claude/CLAUDE.md`: native builds always run with
  `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`, Maven `-T 8`
  at most, one heavy job at a time.

## 3. Module and artifact layout

Single Maven reactor, root `org.sitenetsoft:quarkus-topaz-parent`, importing `io.quarkus:quarkus-bom`.
Scaffold with `mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1:create-extension` and then add the
extra modules by hand.

| Module | Artifact | Role |
|---|---|---|
| `grpc/generator` | not published; profile `generate` only | protos exported from buf, `quarkus-maven-plugin` `generate-code` producing Mutiny and blocking stubs |
| `grpc/stubs` | `quarkus-topaz-grpc` | **committed** generated sources for `aserto.authorizer.v2.*` and `aserto.directory.*.v3`, Jandex-indexed; depends on `quarkus-grpc` and `quarkus-arc` only |
| `runtime` | `quarkus-topaz` | client beans, config, TLS, API key, health, tracing wrap, Dev UI JSON-RPC |
| `deployment` | `quarkus-topaz-deployment` | build steps, Dev Services, Dev UI card, native registrations |
| `security/runtime` | `quarkus-topaz-security` | `HttpSecurityPolicy`, annotations, mappers, interceptors |
| `security/deployment` | `quarkus-topaz-security-deployment` | annotation scanning, bean registration |
| `zanzibar/runtime` | `quarkus-topaz-zanzibar` | `RelationshipManager` over the Directory |
| `zanzibar/deployment` | `quarkus-topaz-zanzibar-deployment` | feature + bean registration |
| `integration-tests` | not published | `@QuarkusTest` and `@QuarkusIntegrationTest` apps, including native |
| `docs` | not published | Antora site, generated config reference |

Rules:

- Every runtime module runs `quarkus-extension-maven-plugin:extension-descriptor` with its own
  `META-INF/quarkus-extension.yaml`, and every runtime and deployment module runs the
  `quarkus-extension-processor` annotation processor. The deployment coordinates in the descriptor
  must match the published artifactIds exactly; verify after the first publish with a scratch consumer
  (the artifactId trap documented in quarkus-tus).
- `quarkus-topaz-security` depends on `quarkus-topaz`, `quarkus-vertx-http`, `quarkus-security`, and
  optionally `quarkus-rest` (provided scope, for the after-match path-parameter access).
- `quarkus-topaz-zanzibar` depends on `quarkus-topaz` and `io.quarkiverse.zanzibar:quarkus-zanzibar`.
  It is the only module touching Quarkiverse artifacts, so client-only users never pull them.
- `quarkus-topaz` depends on `quarkus-grpc`, `quarkus-mutiny`, `quarkus-vertx`,
  `quarkus-tls-registry`, `quarkus-devservices` (runtime marker), optional `quarkus-smallrye-health`.
  It must not depend on `quarkus-security`. Note that `quarkus-grpc` itself depends on
  `quarkus-vertx-http` (the gRPC server shares the HTTP server); that transitive dependency is
  accepted (my call) because `Channels`, `GrpcClientInterceptorContainer` and the Mutiny stub support
  live in `quarkus-grpc`, and re-implementing interceptor discovery and TLS resolution on
  `quarkus-grpc-common` alone would cost more than it saves. With no `@GrpcService` beans the gRPC
  server does not start, so client-only consumers see no behavioural change.
- Package roots: `org.sitenetsoft.quarkus.topaz`, `.topaz.security`, `.topaz.zanzibar`,
  `.topaz.deployment` and so on. Generated stubs keep their proto packages. The Topaz protos declare no `java_package`, so the
  classes land in `aserto.authorizer.v2`, `aserto.authorizer.v2.api`, `aserto.directory.common.v3`,
  `aserto.directory.reader.v3`, `aserto.directory.writer.v3`, `aserto.directory.model.v3`,
  `aserto.directory.importer.v3`, `aserto.directory.exporter.v3`. Google API protos (`google/api/*`)
  are not generated; they resolve to the BOM-managed `com.google.api.grpc:proto-google-common-protos`
  through `quarkus.generate-code.grpc.scan-for-imports`. The grpc-gateway OpenAPI option protos the
  Topaz files import are exported and generated alongside (package
  `grpc.gateway.protoc_gen_openapiv2.options`), as quarkus-authzed-client does.

### Code generation flow

`bin/generate.sh` runs `buf export` (via the `bufbuild/buf` container image, so no local buf install
is needed) for `buf.build/aserto-dev/authorizer:<tag>`, `buf.build/aserto-dev/directory:<tag>` and
`buf.build/grpc-ecosystem/grpc-gateway` into `grpc/generator/src/main/proto`, deletes the exported
`google/` tree and any `aserto/directory/*/v2` directories, then
`mvn -Pgenerate -pl grpc/generator package`, then copies `target/generated-sources/grpc` into
`grpc/stubs/src/main/java`. Pinned tags live in `.github/project.yml`-style properties in the root
pom. A `generate.yml` workflow reruns it and opens a PR on demand. Consumers never run protoc. This is
the quarkus-authzed-client recipe and avoids the two documented pitfalls: Quarkus will not compile
protos found in a runtime jar unless the app opts in with `scan-for-proto`, and
`GrpcCommonProcessor` only registers protobuf classes for native reflection when they are in the
Jandex index.

## 4. Client extension: `quarkus-topaz`

### Transport

Mutiny stubs over a `io.grpc.Channel` built by a `@Recorder` at `RUNTIME_INIT` from `quarkus.topaz.*`
config. Two channels, one per Topaz endpoint (authorizer and directory are separate ports and can be
separate hosts). **The channel is the Vert.x gRPC client** (`io.vertx.grpc.client.GrpcClient` wrapped
in `GrpcClientChannel`), not `NettyChannelBuilder`. Reasons: it honours the TLS registry, it shares the
Quarkus event loop, and it is the only transport left in Quarkus 4.0. This is my call; the risk and
its spike are in §11.

Channel wrapping, in order:

1. API key interceptor: `authorization: basic <key>` metadata on every call (Topaz's
   `topazd/authentication/middleware.go` requires the `basic` scheme). Optional `aserto-tenant-id`
   header for people pointing at a legacy Aserto deployment; Topaz ignores it.
2. Global client interceptors from `GrpcClientInterceptorContainer.getSortedGlobalInterceptors()`,
   which is how `quarkus-opentelemetry` (`GrpcTracingClientInterceptor`) and Micrometer instrument
   channels that `quarkus-grpc` did not create. No OpenTelemetry-specific code in this extension.
3. Deadline from `quarkus.topaz.<service>.deadline`.

Deployment produces `ExtensionSslNativeSupportBuildItem` and `IndexDependencyBuildItem` for
`quarkus-topaz-grpc`.

### Construction outside CDI

`TopazClient` has a public builder usable without Quarkus running:

```java
TopazClient client = TopazClient.builder()
        .authorizer("localhost", 8282).directory("localhost", 9292)
        .plainText(true).apiKey("...").vertx(vertx)   // vertx optional; a private instance is created and owned if absent
        .build();
```

The CDI beans are produced from the same builder by the recorder. The builder exists for three
callers: the Dev Service post-start hook in the deployment module (no CDI, no Quarkus Vert.x at build
time, so it passes a throwaway `Vertx` it closes afterwards), `@QuarkusIntegrationTest` classes that
seed directory data from the test JVM against a native binary, and users who need a second Topaz
endpoint. It is public API and versioned like the rest. This is my call; the alternative of
`grpc-netty-shaded` in deployment scope only was rejected to keep one transport implementation.

### Beans

Synthetic, `@ApplicationScoped`, runtime-init:

```java
@Inject TopazClient topaz;            // umbrella: authorizer(), directory(), blocking()
@Inject AuthorizerClient authorizer;  // also injectable directly
@Inject DirectoryClient directory;    // also injectable directly
```

`AuthorizerClient` exposes the raw Mutiny stub (`MutinyAuthorizerGrpc.MutinyAuthorizerStub stub()`)
and convenience methods that build the request types:

```java
Uni<IsResponse>     is(IdentityContext id, String policyPath, List<String> decisions, Struct resource);
Uni<Boolean>        isAllowed(IdentityContext id, String policyPath, String decision, Map<String,?> resource);
Uni<DecisionTreeResponse> decisionTree(...);  Uni<QueryResponse> query(...);
Uni<List<Module>>   listPolicies();           Uni<Module> getPolicy(String id);  Uni<InfoResponse> info();
```

plus static factories `IdentityContexts.none()`, `.subject(String)`, `.jwt(String)`, `.manual(String)`.

`DirectoryClient` exposes `reader()`, `writer()`, `model()`, `importer()`, `exporter()` as raw Mutiny
stubs and convenience methods for the common path:

```java
Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType, String subjectId);
Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType, String subjectId, String subjectRelation);
Uni<List<Boolean>> checks(List<CheckRequest>);
Uni<Object>  getObject(type, id);   Uni<Void> setObject(Object);   Uni<Void> deleteObject(type, id, withRelations);
Uni<Relation> getRelation(...);     Uni<Void> setRelation(Relation);  Uni<Void> deleteRelation(...);
Uni<String>  getManifest();         Uni<Void> setManifest(String yaml);   Uni<Void> deleteManifest();
Uni<ImportSummary> importData(Multi<ImportRequest>);   Multi<ExportResponse> export(ExportOptions);
```

`blocking()` returns a facade with the same method set using the blocking generated stubs. Manifest
and import/export are streams in the proto; the facade hides the chunking for manifest and exposes
`Multi` for import and export.

### Configuration (`quarkus.topaz.*`, runtime phase unless stated)

```
quarkus.topaz.api-key                        # shared default, optional
quarkus.topaz.tls-configuration-name         # shared default, optional
quarkus.topaz.plain-text                     # shared default; absent = TLS iff a TLS config applies (quarkus-grpc rule)
quarkus.topaz.tenant-id                      # optional, legacy Aserto header
quarkus.topaz.authorizer.host=localhost  .port=8282  .api-key  .tls-configuration-name  .plain-text  .deadline
quarkus.topaz.directory.host=localhost   .port=9292  (same overrides)
quarkus.topaz.health.enabled=true            # build time
quarkus.topaz.devservices.*                  # build time, §6
```

TLS resolution copies `io.quarkus.grpc.runtime.supports.Channels`: a named TLS configuration wins;
otherwise the registry default is used only if it defines a trust store or trust-all; otherwise the
JVM defaults. `quarkus.tls.trust-all=true` stays a test-only escape hatch and is documented as such.

### Health

`@Readiness` `AsyncHealthCheck` calling Authorizer `Info` and Directory `GetManifest` (head only),
registered through `HealthBuildItem` only when `Capability.SMALLRYE_HEALTH` is present and
`quarkus.topaz.health.enabled` is true.

### Dev UI

`CardPageBuildItem` under `IsLocalDevelopment` with: connection summary, a link to the Topaz console
(Dev Services container port 8080), a policies page backed by a JSON-RPC service calling
`ListPolicies`, and a manifest page showing `GetManifest`. Same mechanics as quarkus-tus's Dev UI.

## 5. Security extension: `quarkus-topaz-security`

Two enforcement idioms, because Topaz has two engines, and two attachment points, because Quarkus
has two.

### 5.1 Route-level policy enforcement: the `topaz` `HttpSecurityPolicy`

A named `HttpSecurityPolicy` bean, `name() == "topaz"`, registered unremovable. Users attach it two
ways, both standard Quarkus and nothing new to learn:

```properties
quarkus.http.auth.permission.todos.paths=/todos/*
quarkus.http.auth.permission.todos.policy=topaz
quarkus.http.auth.permission.todos.applies-to=jaxrs     # optional: run after REST matching
```

```java
@AuthorizationPolicy(name = "topaz")
@Path("/todos") public class TodoResource { ... }
```

On each request the policy builds an `IsRequest` from three pluggable mappers, each a CDI bean with a
default implementation that users override by providing their own bean (`@Alternative @Priority` or
simply a bean of the interface, resolved with `Instance` and highest priority wins):

- **`IdentityMapper`** `Uni<IdentityContext> map(SecurityIdentity, RoutingContext)`. Default: if the
  identity carries a `TokenCredential` (OIDC `AccessTokenCredential`, SmallRye JWT), type `JWT` with
  the raw token; else if authenticated, type `SUB` with the principal name; else type `NONE`. Anonymous
  handling is configurable: `quarkus.topaz.enforcement.anonymous=deny|evaluate` (default `deny`).
- **`PolicyPathMapper`** `String map(RoutingContext, Optional<ResourceMethodInfo>)`. Default is the
  Aserto middleware convention so existing Topaz policies work unchanged:
  `<policy-root>.<METHOD>.<path with / replaced by . and {param} replaced by __param>`. When the policy
  runs after REST matching (via `@AuthorizationPolicy` or `applies-to=jaxrs`) the JAX-RS template is
  used, so `/todos/{id}` becomes `todoApp.GET.todos.__id`; before matching only the raw path is
  available and the mapper documents that limitation.
- **`ResourceContextMapper`** `Uni<Struct> map(RoutingContext, Optional<ResourceMethodInfo>)`.
  Default: all JAX-RS path parameters when available, plus query parameters. No body reading by
  default; a user mapper can opt in and the deployment will add `RequireBodyHandlerBuildItem` only when
  `quarkus.topaz.enforcement.read-body=true`.

Config:

```
quarkus.topaz.enforcement.policy-root            # required when the policy is used, e.g. todoApp
quarkus.topaz.enforcement.decision=allowed
quarkus.topaz.enforcement.additional-decisions=        # optional list, e.g. visible,enabled
quarkus.topaz.enforcement.anonymous=deny
quarkus.topaz.enforcement.read-body=false
quarkus.topaz.enforcement.timeout=5s
```

Decision: the `Is` request asks for `decision` plus `additional-decisions`. `is == true` for
`decision` permits; anything else, including a gRPC error, a user that Topaz cannot resolve in the
directory, or a timeout, denies and logs at WARN with the policy path. Topaz only returns the decisions
named in the request, so when `additional-decisions` is non-empty the identity is augmented on permit
with an attribute `topaz.decisions` (`Map<String, Boolean>`) so downstream code can read secondary
decisions such as `visible` or `enabled` without a second call; when it is empty no attribute is set.

### 5.2 Method-level annotations

For code that knows what it is protecting, two annotations usable on any CDI bean method or class,
implemented as CDI interceptors (`@InterceptorBinding`, `@Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)`,
after Quarkus Security's own checks), registered with `InterceptorBindingRegistrarBuildItem`:

```java
@TopazAuthorize(path = "todoApp.PUT.todos.__id", decision = "allowed")
public Todo update(@TopazResource("id") String id, Todo todo) { ... }

@TopazCheck(objectType = "todo", relation = "owner", subjectType = "user")
public void delete(@TopazObjectId String id) { ... }
```

- `@TopazAuthorize`: `path` (empty means derive with `PolicyPathMapper` from the current HTTP request
  if one exists, else build-time error), `decision`, `decisions`. Identity from `IdentityMapper`.
  Resource context from `@TopazResource("key")`-annotated parameters (value converted with Jackson to
  a `Struct` value), merged over the `ResourceContextMapper` output.
- `@TopazCheck`: `objectType`, `relation`, `subjectType` (default `user`), optional `objectId`
  constant; the object id comes from the `@TopazObjectId` parameter or from a `@TopazObjectId`-annotated
  field or getter of a parameter. Subject id from `IdentityMapper`'s `SUB`, or for `JWT` from the token
  `sub` claim, overridable by a `SubjectMapper` bean.
- Both are reactive-aware: on a `Uni`/`Multi`-returning method the check is chained; on a blocking
  method the interceptor blocks via `BlockingSecurityExecutor`.
- Failure throws `io.quarkus.security.ForbiddenException` (401 for anonymous via
  `UnauthorizedException`), so Quarkus REST maps it the same as its own checks.

### 5.3 `@PermissionsAllowed` integration

A `SecurityIdentityAugmentor` adds a permission checker to every authenticated identity so that

```java
@PermissionsAllowed("todoApp.GET.todos")
```

resolves lazily by calling `Is` with the permission name as policy path and the configured decision,
and `TopazRelation` permission class for `@PermissionsAllowed(value="owner", permission=TopazRelation.class, params="id")`
mapping to a directory `Check`. Opt-in with `quarkus.topaz.enforcement.permissions.enabled=true`
(default false) so that users relying on role-based `@PermissionsAllowed` elsewhere are not surprised by
remote calls. This is the lowest-priority piece of the security module and ships last.

### What the security module does not do

- It does not register a global (unnamed) policy. Nothing is enforced unless the user binds the
  `topaz` policy or uses an annotation.
- It does not cache decisions. Topaz is local-first and sub-millisecond; caching is a policy decision
  for the user. A hook point (`DecisionCache` interface with a no-op default) is left so one can be
  added without an API break.

## 6. Dev Services

In `quarkus-topaz-deployment`, `@BuildSteps(onlyIf = {IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class})`,
using the builder-based `DevServicesResultBuildItem.owned()...startable(...)` API (the
`RunningDevService` constructor path is deprecated since 3.25 and gone in 4.0). Skipped when
`quarkus.topaz.authorizer.host` or `quarkus.topaz.directory.host` is set by the user, or when no
container runtime is available (`DockerStatusBuildItem`). Shared container discovery through
`ContainerLocator` on label `quarkus-dev-service-topaz`, and Compose discovery via `ComposeLocator`
for images named `topaz`.

Container: `GenericContainer` on `ghcr.io/aserto-dev/topaz:<pinned>`, exposing 8282, 9292, 9494 and
8080 (console). On the shared network when required. Startup:

1. The Dev Service writes a **version 2 `config.yaml`** into a temp directory under the project
   `target/` (not `/tmp`, per host rules) and bind-mounts it at `/config`. The config enables the
   authorizer, the directory with a BoltDB edge store under `/db`, the console, API key auth only when
   `devservices.api-key` is set, and **plaintext gRPC and gateway listeners**. Topaz's bundled
   self-signed certificates are not used, so tests need no trust-store gymnastics.
2. Policies, exactly one of:
   - `quarkus.topaz.devservices.policy.directory=<path>`: a Rego directory, bind-mounted at `/policies`
     and configured as `opa.local_bundles.paths` with `skip_verification: true`. Classpath or filesystem
     prefixes like the OpenFGA Dev Service (`classpath:policies`, `filesystem:../policies`).
   - `quarkus.topaz.devservices.policy.image=<oci ref>`: an OCI policy image, configured under
     `opa.config.services.ghcr` + `opa.config.bundles`.
   - Neither: Topaz starts with no policy; `Is` calls return errors until one is set. Logged at startup.
3. Wait strategy: a custom `AbstractWaitStrategy` opening a plaintext channel to 9494 and calling
   `grpc.health.v1.Health/Check` until `SERVING`, falling back to `Wait.forListeningPorts(8282, 9292)`
   plus the Topaz "serving" log line if the health port is disabled. Startup timeout from
   `quarkus.devservices.timeout`.
4. Post-start hook, using `TopazClient.builder()` (§4) with a throwaway `Vertx` against the mapped
   ports:
   `SetManifest` from `devservices.manifest-location` and `Import` from `devservices.data-location`
   (a directory of JSON files in Topaz's export format, objects then relations). When a shared
   container was discovered rather than started, the hook compares the manifest and logs a drift
   warning rather than overwriting, as the OpenFGA Dev Service does.
5. Config provider injects `quarkus.topaz.authorizer.host/port`, `quarkus.topaz.directory.host/port`,
   `quarkus.topaz.plain-text=true`, and `quarkus.topaz.api-key` when set, plus
   `quarkus.topaz.devservices.console-url` for the Dev UI link.

Config keys (build time, `quarkus.topaz.devservices.*`): `enabled=true`, `image-name`, `shared=true`,
`service-name=topaz`, `policy.directory`, `policy.image`, `manifest-location`, `data-location`,
`api-key`, `container-env.*`, `console.enabled=true`, `decision-logs.enabled=false`.

## 7. Zanzibar connector: `quarkus-topaz-zanzibar`

One `@ApplicationScoped` `ZanzibarTopazRelationshipManager implements io.quarkiverse.zanzibar.RelationshipManager`
over `DirectoryClient`: `check` maps to Reader `Check`, `add` to Writer `SetRelation` (idempotent in
Topaz, so duplicate adds are fine), `remove` to `DeleteRelation` (missing relation ignored). Subject
relation is not representable in quarkus-zanzibar's `Relationship` record and is left empty.
Deployment registers `FeatureBuildItem("topaz-zanzibar")` and the bean. Docs page shows the
`@FGARelation` flow against a Topaz manifest. This module is also the candidate for upstreaming as
`zanzibar-topaz` in quarkiverse/quarkus-zanzibar later; keeping it separate here makes that a copy, not
a refactor.

## 8. Testing

- `deployment/src/test`: `QuarkusUnitTest` with the Dev Service for client behaviour, TLS via a
  Topaz container configured with test certificates, API key enforcement, config validation (missing
  `policy-root` when the `topaz` policy is bound is a build-time error), shared-container drift.
  `QuarkusDevModeTest` for Dev UI JSON-RPC.
- `security/deployment/src/test`: `QuarkusUnitTest` apps exercising both attachment points and both
  annotations with the bundled sample policy and manifest, including deny on unresolved user, deny on
  Topaz down, anonymous handling, reactive and blocking methods.
- `integration-tests`: a `todo` application mirroring Topaz's own `todo` template (policy, manifest,
  users) with `@QuarkusTest` classes run by Surefire and `*IT extends TestBase` run by Failsafe under a
  `native` profile, with the mandatory native build args. The same app is the documentation example.
- CI (GitHub Actions): `mvn -T 4 verify`, a second deployment-test run against the previous Topaz
  minor to catch drift, and a native job. Workflows are the `quarkus-tus` ones adapted to Maven.

## 9. Documentation

Antora module like quarkus-tus (`docs/antora.yml`, `docs/modules/ROOT/pages`): `index`,
`client`, `security` (route and method enforcement, mappers, policy path convention table),
`dev-services`, `zanzibar-connector`, `configuration` (generated by `quarkus-config-doc-maven-plugin`),
`native`, `testing`, `releasing`. Design specs and plans live in `docs/design/`. A `CLAUDE.md` at the
root points at the `ai-skills` Quarkus and skill-authoring graphs and records the build rules.

## 10. Decisions already made, and by whom

| Decision | Owner |
|---|---|
| Standalone Topaz extension, full Topaz coverage, not a quarkus-zanzibar connector | user |
| Zanzibar connector exists but is a separate, optional artifact | user |
| `org.sitenetsoft` groupId, Maven | user |
| Quarkus 3.40.1, Java 25 toolchain, Antora docs, specs in `docs/design` | carried from quarkus-tus |
| Generate stubs from buf modules with quarkus-grpc, commit the output, no `com.aserto` deps | me |
| Vert.x gRPC client channel rather than `NettyChannelBuilder` | me |
| Bytecode release 21 rather than 25, for consumer reach | me, flagged |
| Three extensions (client, security, zanzibar) in one reactor | me |
| Named `topaz` policy plus annotations; no global policy; fail closed | me |
| Aserto middleware policy-path convention as the default | me |
| Dev Service runs plaintext listeners and generates its own config | me |
| Pinned proto tags and image tag, bumped by PR | me |
| Accept `quarkus-vertx-http` transitively via `quarkus-grpc` | me |
| Public non-CDI `TopazClient.builder()`; deployment uses it with a throwaway Vert.x | me |
| Route policy gates on one decision, optional `additional-decisions` fill the identity attribute | me |

## 11. Risks and spikes before the plan

1. **Plaintext gRPC in the Topaz container.** Topaz enables TLS on gRPC by default with generated
   certs. The config supports listeners without certs, but this must be confirmed on 0.33.22 before the
   Dev Service design is final. Fallback: let Topaz generate certs into a bind-mounted `/certs`
   directory and register its CA through `TlsCertificateBuildItem` so the client trusts it
   automatically. Half-day spike.
2. **Vert.x gRPC client with Mutiny stubs on 3.40.1.** `GrpcClientChannel` exists in
   `vertx-grpc-client` 4.5.x and `quarkus-grpc` uses it when `use-quarkus-grpc-client=true`; confirm
   bidi streaming (Importer) and server streaming (Exporter, GetManifest) work through it. Fallback:
   `NettyChannelBuilder` behind the same recorder interface, with TLS converted from the registry's
   `SSLContext`. Half-day spike, can share the Dev Service spike's container.
3. **Identity resolution.** For `SUB` and `JWT` identity types Topaz requires the user to exist in
   the directory and errors otherwise. The security docs must say this loudly, and the integration test
   must cover the "authenticated but unknown to Topaz" case as a deny.
4. **Protobuf 4.x.** Quarkus 3.40.1 manages protobuf-java 4.35.0; generated code from protoc 4.x is
   required. The generator module inherits protoc from the Quarkus BOM, so this is a check, not work.
5. **Topaz is 0.x.** Directory v3 is stable but Topaz makes breaking changes in patch releases
   (v0.33.15 removed RPCs). The dual-version CI job and pinned image are the mitigation.

## 12. Left to the builder

- Exact internal class layout, recorder signatures, and build item ordering.
- Dev UI component details beyond the four pages listed.
- Blocking facade naming and whether it is a nested class or a sibling interface.
- CI matrix beyond the two Topaz versions and native job.
- Release workflow details (Sonatype Central vs GitHub Packages), to be copied from quarkus-tus's
  `releasing.adoc` once the first version is ready.
