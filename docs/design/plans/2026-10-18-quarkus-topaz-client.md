# Quarkus Topaz Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `org.sitenetsoft:quarkus-topaz`, the client extension of the design: generated gRPC stubs, injectable Authorizer and Directory clients, API key and TLS support, a Dev Service that boots Topaz with the user's policies and manifest, health, tracing, Dev UI, native support, docs and CI.

**Architecture:** A Maven reactor. `grpc/stubs` holds committed Mutiny and blocking stubs generated from the pinned buf modules. `runtime` builds two Vert.x gRPC channels (authorizer, directory) from `quarkus.topaz.*` config through a public `TopazClient.builder()` that also works without CDI; a recorder exposes the client as synthetic beans. `deployment` adds build steps, the Dev Service (container, generated config, readiness wait, manifest and data load), health registration and the Dev UI card. The security and Zanzibar extensions of the design are separate plans that build on this one's `TopazClient` API.

**Tech Stack:** Java 25 toolchain, bytecode release 21, Maven 3.9, Quarkus 3.40.1 (`quarkus-grpc`, `quarkus-tls-registry`, `quarkus-devservices`, `quarkus-smallrye-health` optional), Vert.x 4.5 gRPC client, Testcontainers 2.0 via Podman, buf (container image) for proto export, Antora docs.

**Spec:** `docs/design/specs/2026-10-18-quarkus-topaz-design.md` (sections 1 to 4, 6, 8, 9, 11). Sections 5 and 7 are out of scope here and get their own plans once this one lands.

## Global Constraints

- Quarkus `3.40.1`; `quarkus.version` property; `io.quarkus:quarkus-bom` imported; nothing else version-pinned except what the BOM does not manage.
- GroupId `org.sitenetsoft`, version `0.1.0-SNAPSHOT`, Apache 2.0 license header-free (LICENSE file only, as quarkus-tus).
- `maven.compiler.release=21`; build with `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64` (the machine's default `java` is 17).
- Protos pinned: `buf.build/aserto-dev/authorizer:v0.24.0`, `buf.build/aserto-dev/directory:v0.34.1`. Topaz image pinned: `ghcr.io/aserto-dev/topaz:0.33.22`.
- No dependency on any `com.aserto:*` artifact. No `grpc-netty-shaded`.
- Container runtime is rootless Podman. Every test or build command that starts containers runs with `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`. `docker` is not installed; the proto export script uses `podman run docker.io/bufbuild/buf`.
- Maven parallelism at most `-T 4` locally; one heavy job at a time; native builds always pass `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`; commit before any native build.
- Scratch files from the Dev Service go under the build output directory, never `/tmp`.
- Fail closed: no convenience method returns `true` on an error; errors propagate as `io.grpc.StatusRuntimeException`.
- Package root `org.sitenetsoft.quarkus.topaz`; runtime internals in `org.sitenetsoft.quarkus.topaz.runtime`; deployment in `org.sitenetsoft.quarkus.topaz.deployment`. Generated stubs keep proto packages (`aserto.authorizer.v2`, `aserto.directory.reader.v3`, ...).
- Feature name `topaz`. Config prefix `quarkus.topaz`. Dev Service label `quarkus-dev-service-topaz`.

## Review Focus

1. Topaz unreachable: a call must fail with `StatusRuntimeException` (UNAVAILABLE or DEADLINE_EXCEEDED) within the configured deadline, never hang; the readiness check must report DOWN, not throw. Tests: Task 7 step 1 (`unreachableFailsWithinDeadline`), Task 9 step 1 (`downWhenUnreachable`).
2. Wrong API key: a Directory call must surface UNAUTHENTICATED; the user-set `quarkus.topaz.api-key` must win over the Dev Service's injected key. Test: Task 8 step 7 (`wrongApiKeyIsUnauthenticated`).
3. Unknown policy path or decision: `isAllowed` must return `false` when the decision is absent from the response and must propagate Topaz's error when the policy does not exist; it must never return `true` by accident. Tests: Task 6 step 1 (`isAllowedFalseWhenDecisionMissing`), Task 8 step 7 (`unknownPolicyPathPropagatesError`).
4. Dev Service with no policy configured: the container still starts and the Directory is usable. Test: Task 8 step 7 (`startsWithoutPolicy`).
5. Dev Service pointing at a missing manifest or data file: the build fails with a `ConfigurationException` naming the key, instead of a container that silently has no model. Test: Task 8 step 7 (`missingManifestFailsBuild`).

---

### Task 1: Reactor scaffold and empty extension

**Files:**
- Create: `pom.xml`, `runtime/pom.xml`, `deployment/pom.xml`, `.gitignore`, `LICENSE`, `README.md`, `CLAUDE.md`
- Create: `runtime/src/main/resources/META-INF/quarkus-extension.yaml`
- Create: `deployment/src/main/java/org/sitenetsoft/quarkus/topaz/deployment/TopazProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/TopazFeatureTest.java`

**Interfaces:**
- Produces: `TopazProcessor.FEATURE = "topaz"`; reactor modules `runtime` (artifact `quarkus-topaz`) and `deployment` (artifact `quarkus-topaz-deployment`); root properties `quarkus.version`, `maven.compiler.release`, `topaz.image` (`ghcr.io/aserto-dev/topaz:0.33.22`), `pb-authorizer.tag`, `pb-directory.tag`.

- [ ] **Step 1: Scaffold with the Quarkus Maven plugin**

Run from the repo root (it already holds `docs/` and `.git`):

```bash
cd /home/jn/Projects/SiteNetSoft/quarkus-topaz && JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 \
mvn -N io.quarkus.platform:quarkus-maven-plugin:3.40.1:create-extension -DgroupId=org.sitenetsoft -DextensionId=topaz -DextensionName="Topaz" -DwithoutTests -DquarkusVersion=3.40.1
```

The plugin writes into a `quarkus-topaz/` subdirectory when run from an existing project; move its contents up one level so `pom.xml` sits beside `docs/`. Delete the generated `integration-tests` module if any (Task 12 recreates it) and remove the `docs` module reference (Task 13 adds it).

- [ ] **Step 2: Fix the root pom**

Set `<version>0.1.0-SNAPSHOT</version>`, artifactId `quarkus-topaz-parent`, `maven.compiler.release` `21`, properties `topaz.image`, `pb-authorizer.tag=v0.24.0`, `pb-directory.tag=v0.34.1`. Modules: `runtime`, `deployment` only for now. Surefire and failsafe `argLine` must include `-Djava.util.logging.manager=org.jboss.logmanager.LogManager` and `-XX:TieredStopAtLevel=1` (host rule for forks). The Podman environment variables come from the shell, not the pom.

- [ ] **Step 3: Write the extension metadata**

`runtime/src/main/resources/META-INF/quarkus-extension.yaml`: name `Topaz`, description `Client, Dev Services and health for the Topaz authorizer and directory (aserto-dev/topaz)`, keywords `topaz`, `aserto`, `authorization`, `rebac`, `opa`, `rego`, `zanzibar`; categories `security`; status `preview`; `config: ["quarkus.topaz."]`; guide `https://sitenetsoft.github.io/quarkus-topaz/`.

- [ ] **Step 4: Write the failing test**

```java
public class TopazFeatureTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses())
            .setLogRecordPredicate(r -> r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> r.getMessage() != null
                    && String.format(r.getMessage(), r.getParameters()).contains("topaz")));
    @Test void featureInstalled() { }
}
```

Dependencies in `deployment/pom.xml` test scope: `io.quarkus:quarkus-junit-internal`, `org.assertj:assertj-core`.

- [ ] **Step 5: Run it to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -q -pl deployment -am test -Dtest=TopazFeatureTest`
Expected: FAIL (no feature named topaz logged, or `TopazProcessor` missing).

- [ ] **Step 6: Implement `TopazProcessor`**

`class TopazProcessor { static final String FEATURE = "topaz"; @BuildStep FeatureBuildItem feature() }`.

- [ ] **Step 7: Run it to verify it passes**

Same command. Expected: PASS.

- [ ] **Step 8: Write `.gitignore`, `README.md`, `CLAUDE.md`, `LICENSE`**

`.gitignore`: `target/`, `.idea/`, `*.iml`, `.vscode/`. `README.md`: one paragraph, status "pre-release", link to the spec. `CLAUDE.md`: the Global Constraints of this plan verbatim plus pointers to `~/Projects/SiteNetSoft/ai-skills/dev-practices/quarkus/index.md` and `meta/skill-authoring/index.md`. `LICENSE`: Apache 2.0 text copied from `~/Projects/SiteNetSoft/quarkus-tus/LICENSE`.

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "Scaffold quarkus-topaz reactor with runtime and deployment modules"
```

---

### Task 2: Generated gRPC stubs module

**Files:**
- Create: `grpc/pom.xml` (artifact `quarkus-topaz-grpc-parent`, modules `stubs`, and `generator` under profile `generate`)
- Create: `grpc/generator/pom.xml`, `grpc/generator/src/main/resources/application.properties`
- Create: `grpc/stubs/pom.xml` (artifact `quarkus-topaz-grpc`), `grpc/stubs/src/main/java/**` (generated, committed)
- Create: `bin/generate.sh`
- Test: `grpc/stubs/src/test/java/org/sitenetsoft/quarkus/topaz/grpc/StubsSmokeTest.java`

**Interfaces:**
- Produces: classes `aserto.authorizer.v2.MutinyAuthorizerGrpc` (with `newMutinyStub(Channel)` and `MutinyAuthorizerStub`), `aserto.authorizer.v2.AuthorizerGrpc` (blocking `newBlockingStub`, `AuthorizerImplBase`), `aserto.authorizer.v2.IsRequest/IsResponse/Decision/...`, `aserto.authorizer.v2.api.IdentityContext/IdentityType/PolicyContext`, `aserto.directory.reader.v3.MutinyReaderGrpc`, `...writer.v3.MutinyWriterGrpc`, `...model.v3.MutinyModelGrpc`, `...importer.v3.MutinyImporterGrpc`, `...exporter.v3.MutinyExporterGrpc`, `aserto.directory.common.v3.Object/Relation/ObjectIdentifier/RelationIdentifier`.

- [ ] **Step 1: Write `bin/generate.sh`**

Mirror `quarkus-authzed-client/bin/generate.sh` with these changes: use `podman run --rm --user $(id -u):$(id -g) -e HOME=/tmp -v "$PWD:/workspace:Z" -w /workspace docker.io/bufbuild/buf export <module> -o grpc/generator/src/main/proto`; export `buf.build/grpc-ecosystem/grpc-gateway`, `buf.build/aserto-dev/authorizer:${PB_AUTHORIZER_TAG}`, `buf.build/aserto-dev/directory:${PB_DIRECTORY_TAG}` (tags read from the root pom properties with `mvn help:evaluate -q -DforceStdout`); then `rm -rf grpc/generator/src/main/proto/google` (resolved from `proto-google-common-protos` instead) and `rm -rf grpc/generator/src/main/proto/aserto/directory/*/v2`; keep only `protoc-gen-openapiv2/options/*.proto` from the grpc-gateway export; run `mvn -Pgenerate -pl grpc/generator clean package`; replace `grpc/stubs/src/main/java/{aserto,grpc}` with `grpc/generator/target/generated-sources/grpc/*`.

- [ ] **Step 2: Write the generator module**

`grpc/generator/pom.xml`: packaging jar, deps `io.quarkus:quarkus-arc`, `io.quarkus:quarkus-grpc`, `com.google.api.grpc:proto-google-common-protos`; `quarkus-maven-plugin` with `<extensions>true</extensions>` and goals `build`, `generate-code`. `application.properties`: `quarkus.generate-code.grpc.scan-for-imports=com.google.protobuf:protobuf-java,com.google.api.grpc:proto-google-common-protos`.

- [ ] **Step 3: Write the stubs module**

`grpc/stubs/pom.xml`: deps `io.quarkus:quarkus-grpc`, `io.quarkus:quarkus-arc`, `com.google.api.grpc:proto-google-common-protos`; `io.smallrye:jandex-maven-plugin` `jandex` goal; compiler release 21; `maven-javadoc-plugin` skipped for this module.

- [ ] **Step 4: Run the generation**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 bin/generate.sh`
Expected: `grpc/stubs/src/main/java/aserto/authorizer/v2/MutinyAuthorizerGrpc.java` and `grpc/stubs/src/main/java/aserto/directory/reader/v3/MutinyReaderGrpc.java` exist; no `aserto/directory/*/v2` directory; no `google/` directory.

- [ ] **Step 5: Write the failing smoke test**

```java
class StubsSmokeTest {
    @Test void authorizerStubsExist() {
        assertThat(MutinyAuthorizerGrpc.SERVICE_NAME).isEqualTo("aserto.authorizer.v2.Authorizer");
        assertThat(IsRequest.newBuilder().setPolicyContext(PolicyContext.newBuilder().setPath("todoApp.GET.todos").addDecisions("allowed")).build()
                .getPolicyContext().getDecisions(0)).isEqualTo("allowed");
    }
    @Test void directoryServicesExist() {
        assertThat(List.of(MutinyReaderGrpc.SERVICE_NAME, MutinyWriterGrpc.SERVICE_NAME, MutinyModelGrpc.SERVICE_NAME,
                MutinyImporterGrpc.SERVICE_NAME, MutinyExporterGrpc.SERVICE_NAME))
            .containsExactly("aserto.directory.reader.v3.Reader", "aserto.directory.writer.v3.Writer",
                "aserto.directory.model.v3.Model", "aserto.directory.importer.v3.Importer", "aserto.directory.exporter.v3.Exporter");
    }
    @Test void noV2Directory() { assertThatThrownBy(() -> Class.forName("aserto.directory.reader.v2.ReaderGrpc")).isInstanceOf(ClassNotFoundException.class); }
}
```

- [ ] **Step 6: Run it**

Run: `JAVA_HOME=... mvn -q -pl grpc/stubs test`
Expected: PASS (if `SERVICE_NAME` is not a constant on the Mutiny class, use `AuthorizerGrpc.SERVICE_NAME` etc.; adjust the test, not the generated code).

- [ ] **Step 7: Add `grpc` to the root modules and commit**

```bash
git add -A && git commit -m "Add generated Topaz authorizer v2 and directory v3 gRPC stubs"
```

---

### Task 3: Spike: plaintext Topaz container and Vert.x channel streaming

This task produces knowledge, not product code. Its output is a decision recorded in the spec's section 11 and a committed throwaway test that later tasks delete.

**Files:**
- Create: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/spike/TopazPlaintextSpikeTest.java`
- Create: `deployment/src/test/resources/spike/config.yaml`
- Modify: `docs/design/specs/2026-10-18-quarkus-topaz-design.md` section 11

- [ ] **Step 1: Write the candidate config**

`version: 2`; `logging: {prod: true, log_level: info}`; `directory: {db_path: /db/topaz.db}`; `remote_directory: {address: "0.0.0.0:9292", insecure: false, no_tls: true}` (Topaz rejects `insecure: true` together with `no_tls: true` as mutually exclusive; confirmed in Task 3); `auth.options.default: {enable_api_key: false, enable_anonymous: true}`; `api.health.listen_address: "0.0.0.0:9494"`; `api.services`: `reader`, `writer`, `model`, `importer`, `exporter` each `grpc.listen_address: "0.0.0.0:9292"` with **no `certs` block**, `gateway.listen_address: "0.0.0.0:9393"`, `gateway.http: true`; `authorizer` `grpc.listen_address: "0.0.0.0:8282"` no certs, gateway 8383 `http: true`; `console` grpc 8081, gateway 8080 `http: true`; `opa: {instance_id: "-", graceful_shutdown_period_seconds: 2, local_bundles: {paths: ["/policies"], skip_verification: true}}`.

- [ ] **Step 2: Write the spike test (plain JUnit + Testcontainers, no Quarkus)**

Start `GenericContainer("ghcr.io/aserto-dev/topaz:0.33.22")` with the config bind-mounted at `/config/config.yaml`, an empty temp dir at `/policies`, env `TOPAZ_DB_DIR=/db`, exposed ports 8282, 9292, 9494, wait `Wait.forListeningPorts(8282, 9292)`. Create `Vertx vertx = Vertx.vertx()`, `GrpcClient client = GrpcClient.client(vertx)`, `Channel ch = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(mappedPort(8282), host))`. Assert:
- `AuthorizerGrpc.newBlockingStub(ch).info(InfoRequest.getDefaultInstance()).getVersion()` is non-empty (plaintext unary works).
- On the directory channel, `MutinyModelGrpc.newMutinyStub(dirCh).setManifest(Multi.createFrom().item(SetManifestRequest.newBuilder().setBody(Body.newBuilder().setData(ByteString.copyFromUtf8(MANIFEST))).build()))` completes (client streaming), then `getManifest(GetManifestRequest.getDefaultInstance()).collect().asList()` contains a `Body` whose data equals `MANIFEST` (server streaming).
- `MutinyImporterGrpc.newMutinyStub(dirCh).import_(Multi of two ImportRequest: one Object user `rick`, one Relation)` completes and `MutinyReaderGrpc...check(...)` returns `true` (bidi streaming).

`MANIFEST` is the three-line model used everywhere in this plan:

```yaml
model:
  version: 3
types:
  user: {}
  todo:
    relations:
      owner: user
    permissions:
      can_delete: owner
```

- [ ] **Step 3: Run it**

Run: `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -q -pl deployment -am test -Dtest=TopazPlaintextSpikeTest`
Expected: PASS. If Topaz refuses to start without `certs`, read its log: if there is a boolean to disable TLS per listener, use it and re-run; if plaintext is impossible, stop and report. The fallback (Topaz-generated certs trusted through `TlsCertificateBuildItem`) changes Tasks 5, 7 and 8 and must be re-planned before continuing.

- [ ] **Step 4: Record the result**

Update spec section 11 items 1 and 2 with "confirmed on 0.33.22, <date>" and the exact config keys that worked. Commit: `git commit -am "Spike: confirm plaintext Topaz listeners and Vert.x gRPC streaming"`. The spike test stays until Task 8 replaces it.

---

### Task 4: Runtime configuration

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/TopazConfig.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/TopazServiceConfig.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/ResolvedServiceSettings.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/topaz/runtime/ResolvedServiceSettingsTest.java`

**Interfaces:**
- Produces:

```java
@ConfigMapping(prefix = "quarkus.topaz") @ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TopazConfig {
    Optional<String> apiKey(); Optional<String> tlsConfigurationName(); Optional<Boolean> plainText();
    Optional<String> tenantId(); Optional<Duration> deadline(); Optional<String> consoleUrl();
    TopazServiceConfig authorizer(); TopazServiceConfig directory();
}
@ConfigGroup public interface TopazServiceConfig {
    @WithDefault("localhost") String host(); OptionalInt port(); Optional<String> apiKey();
    Optional<String> tlsConfigurationName(); Optional<Boolean> plainText(); Optional<Duration> deadline();
}
public record ResolvedServiceSettings(String host, int port, Optional<String> apiKey, Optional<String> tlsConfigurationName,
        Optional<Boolean> plainText, Optional<Duration> deadline) {
    public static ResolvedServiceSettings resolve(TopazConfig root, TopazServiceConfig service, int defaultPort);
}
```

Resolution: per-service value wins, then root value, then default. Default ports `8282` authorizer, `9292` directory. `consoleUrl` is documented as "set by Dev Services" and never read by the client.

- [ ] **Step 1: Write the failing test**

Build `TopazConfig` instances with SmallRye Config: `new SmallRyeConfigBuilder().withMapping(TopazConfig.class).withSources(new PropertiesConfigSource(Map.of(...), "test")).build().getConfigMapping(TopazConfig.class)`.

```java
@Test void defaultsPerService() { var s = ResolvedServiceSettings.resolve(cfg(Map.of()), cfg(Map.of()).authorizer(), 8282);
    assertThat(s.host()).isEqualTo("localhost"); assertThat(s.port()).isEqualTo(8282); assertThat(s.plainText()).isEmpty(); }
@Test void serviceOverridesRoot() { var c = cfg(Map.of("quarkus.topaz.api-key","root","quarkus.topaz.directory.api-key","dir","quarkus.topaz.directory.port","19292"));
    assertThat(ResolvedServiceSettings.resolve(c, c.directory(), 9292)).extracting("apiKey","port").containsExactly(Optional.of("dir"), 19292);
    assertThat(ResolvedServiceSettings.resolve(c, c.authorizer(), 8282).apiKey()).contains("root"); }
```

Test deps for `runtime`: JUnit 5, AssertJ, Mockito (`org.mockito:mockito-core`, BOM-managed). SmallRye Config is already transitive.

- [ ] **Step 2: Run to verify it fails** `mvn -q -pl runtime test` → compilation failure.
- [ ] **Step 3: Implement the three types.**
- [ ] **Step 4: Run to verify it passes.**
- [ ] **Step 5: Commit** `git commit -am "Add quarkus.topaz runtime configuration"`

---

### Task 5: Channel factory and API key interceptor

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/ApiKeyClientInterceptor.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/TopazChannels.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/topaz/runtime/ApiKeyClientInterceptorTest.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/topaz/runtime/TopazChannelsTest.java`

**Interfaces:**
- Consumes: `ResolvedServiceSettings` (Task 4), `io.quarkus.tls.TlsConfiguration`, `io.vertx.grpc.client.GrpcClient`, `io.vertx.grpc.client.GrpcClientChannel`, `io.quarkus.tls.runtime.config.TlsConfigUtils.configure(HttpClientOptions, TlsConfiguration)`.
- Produces:

```java
public final class ApiKeyClientInterceptor implements ClientInterceptor {
    public static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> TENANT_ID = Metadata.Key.of("aserto-tenant-id", ASCII_STRING_MARSHALLER);
    public ApiKeyClientInterceptor(String apiKey, Optional<String> tenantId);  // sends "basic " + apiKey
}
public final class TopazChannels {
    public static boolean useTls(ResolvedServiceSettings s, Optional<TlsConfiguration> named, Optional<TlsConfiguration> registryDefault);
    public static ManagedChannelHolder create(Vertx vertx, ResolvedServiceSettings s, Optional<TlsConfiguration> tls, List<ClientInterceptor> interceptors);
    public record ManagedChannelHolder(Channel channel, GrpcClient client) implements AutoCloseable { }
}
```

`useTls` rule (spec section 4): `plainText` present → `!plainText`; else named present → true; else registry default present and (`getTrustStoreOptions() != null` or `isTrustAll()`) → true; else false. `create` builds `GrpcClientOptions` + `HttpClientOptions` (`TlsConfigUtils.configure` when TLS, `setSsl(true)`), `GrpcClient.client(vertx, options)`, `new GrpcClientChannel(client, SocketAddress.inetSocketAddress(port, host))`, wraps with `ClientInterceptors.intercept(channel, interceptors)` where `interceptors` already includes the API key interceptor when `apiKey` is present.

- [ ] **Step 1: Write the failing interceptor test**

Use `io.grpc:grpc-inprocess` (test scope) and `AuthorizerGrpc.AuthorizerImplBase` with a `ServerInterceptor` that captures `Metadata`:

```java
@Test void sendsBasicApiKeyAndTenant() {
    // in-process server named "t"; blocking stub over ClientInterceptors.intercept(channel, new ApiKeyClientInterceptor("k1", Optional.of("ten")))
    stub.info(InfoRequest.getDefaultInstance());
    assertThat(captured.get(ApiKeyClientInterceptor.AUTHORIZATION)).isEqualTo("basic k1");
    assertThat(captured.get(ApiKeyClientInterceptor.TENANT_ID)).isEqualTo("ten");
}
```

- [ ] **Step 2: Write the failing `useTls` tests**

`plainTextTrueWins`, `plainTextFalseForcesTls`, `namedConfigEnablesTls`, `defaultWithTrustStoreEnablesTls`, `defaultWithoutTrustIsPlaintext` with Mockito mocks of `TlsConfiguration`.

- [ ] **Step 3: Run to verify they fail.** `mvn -q -pl runtime test`
- [ ] **Step 4: Implement both classes.**
- [ ] **Step 5: Run to verify they pass.**
- [ ] **Step 6: Commit** `git commit -am "Add Topaz channel factory with TLS resolution and API key interceptor"`

---

### Task 6: `TopazClient`, `AuthorizerClient`, `DirectoryClient`, blocking facade

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/TopazClient.java` (with nested `Builder`)
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/AuthorizerClient.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/DirectoryClient.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/BlockingTopazClient.java`, `BlockingAuthorizerClient.java`, `BlockingDirectoryClient.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/IdentityContexts.java`, `Structs.java`, `ImportSummary.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/topaz/AuthorizerClientTest.java`, `DirectoryClientTest.java`, `StructsTest.java`

**Interfaces:**
- Consumes: Task 5 `TopazChannels`, `ApiKeyClientInterceptor`; Task 2 stubs.
- Produces (public API, exactly these names):

```java
public final class TopazClient implements AutoCloseable {
    public static Builder builder();
    public AuthorizerClient authorizer(); public DirectoryClient directory(); public BlockingTopazClient blocking();
    public void close();   // closes channels; closes Vertx only if the builder created it
    public static final class Builder {
        public Builder authorizer(String host, int port); public Builder directory(String host, int port);
        public Builder plainText(boolean v); public Builder apiKey(String k); public Builder tenantId(String t); public Builder deadline(Duration d);
        public Builder tlsConfiguration(TlsConfiguration tls);
        public Builder vertx(Vertx vertx); public Builder interceptors(List<ClientInterceptor> extra);
        public Builder authorizerChannel(Channel ch); public Builder directoryChannel(Channel ch);  // bypass; for tests and advanced use
        public Builder settings(ResolvedServiceSettings authorizer, ResolvedServiceSettings directory, Optional<TlsConfiguration> tls); // used by the recorder
        public TopazClient build();
    }
}
public final class AuthorizerClient {
    public MutinyAuthorizerGrpc.MutinyAuthorizerStub stub();
    public Uni<IsResponse> is(IsRequest r);
    public Uni<IsResponse> is(IdentityContext id, String policyPath, List<String> decisions, Struct resource);
    public Uni<Boolean> isAllowed(IdentityContext id, String policyPath, String decision, Map<String, ?> resource);
    public Uni<DecisionTreeResponse> decisionTree(DecisionTreeRequest r); public Uni<QueryResponse> query(QueryRequest r); public Uni<CompileResponse> compile(CompileRequest r);
    public Uni<List<Module>> listPolicies(); public Uni<Module> getPolicy(String id); public Uni<InfoResponse> info();
}
public final class DirectoryClient {
    public MutinyReaderGrpc.MutinyReaderStub reader(); /* writer(), model(), importer(), exporter() likewise */
    public Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType, String subjectId);
    public Uni<Boolean> check(String objectType, String objectId, String relation, String subjectType, String subjectId, String subjectRelation);
    public Uni<List<Boolean>> checks(List<CheckRequest> checks);
    public Uni<aserto.directory.common.v3.Object> getObject(String type, String id);
    public Uni<aserto.directory.common.v3.Object> setObject(aserto.directory.common.v3.Object o);
    public Uni<Void> deleteObject(String type, String id, boolean withRelations);
    public Uni<Relation> getRelation(String objectType, String objectId, String relation, String subjectType, String subjectId);
    public Uni<Relation> setRelation(Relation r);
    public Uni<Void> deleteRelation(String objectType, String objectId, String relation, String subjectType, String subjectId);
    public Uni<String> getManifest(); public Uni<Void> setManifest(String yaml); public Uni<Void> deleteManifest();
    public Uni<ImportSummary> importData(Multi<ImportRequest> requests); public Multi<ExportResponse> export(ExportRequest r);
}
public record ImportSummary(long objectsSet, long objectsDeleted, long relationsSet, long relationsDeleted, List<String> errors) { }
public final class IdentityContexts { static IdentityContext none(); subject(String); jwt(String); manual(String); }
public final class Structs { public static Struct of(Map<String, ?> map); public static Value value(Object o); }  // String, Number, Boolean, null, Map, Iterable
```

Semantics: `isAllowed` is `is(...)` with `decisions = List.of(decision)` and returns `true` only if a `Decision` with `getDecision().equals(decision)` has `getIs() == true`; absent → `false`. `setManifest` sends one `SetManifestRequest` with a `Body` of the whole string (Topaz accepts one chunk; chunk at 64 KiB if larger). `getManifest` concatenates all `Body` chunks. Blocking facades call `.await().atMost(deadline or 30s)`.

- [ ] **Step 1: Write the failing `AuthorizerClientTest`**

In-process server with a fake `AuthorizerImplBase` whose `is` records the request and answers from a configurable `IsResponse`:

```java
@Test void isAllowedTrueWhenDecisionTrue()   // response has Decision(allowed, true) → true; request carried identity SUB "rick", path, decisions [allowed], resource {"id":"1"}
@Test void isAllowedFalseWhenDecisionFalse()
@Test void isAllowedFalseWhenDecisionMissing() // response has Decision(visible, true) only → false
@Test void isPropagatesGrpcErrors()           // fake throws Status.NOT_FOUND → Uni fails with StatusRuntimeException NOT_FOUND
@Test void infoAndListPolicies()
```

- [ ] **Step 2: Write the failing `DirectoryClientTest`**

Fakes for Reader (`check` returns per-request configured bool; `checks` echoes), Writer (`setRelation`), Model (`getManifest` streams two chunks "mod" + "el: 3", `setManifest` concatenates the stream and records it), Importer (counts objects and relations and returns one `ImportResponse` with counters):

```java
@Test void checkMapsFields()  // asserts CheckRequest object_type/object_id/relation/subject_type/subject_id
@Test void getManifestConcatenatesChunks()  // "model: 3"
@Test void setManifestSendsBody()
@Test void importDataSummarises()  // 2 objects, 1 relation → ImportSummary(2,0,1,0,[])
```

- [ ] **Step 3: Write `StructsTest`** (`nested map and list round trip`, `null becomes NullValue`).
- [ ] **Step 4: Run to verify they fail.**
- [ ] **Step 5: Implement the classes.** `Builder.build()` creates `Vertx.vertx()` only when neither `vertx` nor both channels were given.
- [ ] **Step 6: Run to verify they pass.** `mvn -q -pl runtime test`
- [ ] **Step 7: Commit** `git commit -am "Add TopazClient with authorizer and directory clients"`

---

### Task 7: Recorder, synthetic beans, native registrations

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/TopazRecorder.java`
- Modify: `deployment/src/main/java/org/sitenetsoft/quarkus/topaz/deployment/TopazProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/TopazBeansTest.java`

**Interfaces:**
- Consumes: `TopazClient.Builder.settings(...)` (Task 6), `TlsRegistryBuildItem.registry()` (`Supplier<TlsConfigurationRegistry>`), `ShutdownContext`, `GrpcClientInterceptorContainer.getSortedGlobalInterceptors()`.
- Produces: beans `TopazClient`, `AuthorizerClient`, `DirectoryClient` (`@ApplicationScoped`, `@Default`); recorder method `RuntimeValue<TopazClient> createClient(TopazConfig config, Supplier<TlsConfigurationRegistry> tls, ShutdownContext shutdown)`.

- [ ] **Step 1: Write the failing test**

```java
public class TopazBeansTest {
    @RegisterExtension static final QuarkusUnitTest TEST = new QuarkusUnitTest()
        .overrideConfigKey("quarkus.topaz.authorizer.port", "1").overrideConfigKey("quarkus.topaz.directory.port", "1")
        .overrideConfigKey("quarkus.topaz.plain-text", "true").overrideConfigKey("quarkus.topaz.deadline", "2s")
        .overrideConfigKey("quarkus.topaz.devservices.enabled", "false");
    @Inject TopazClient client; @Inject AuthorizerClient authorizer; @Inject DirectoryClient directory;
    @Test void beansInjected() { assertThat(client.authorizer()).isSameAs(authorizer); assertThat(client.directory()).isSameAs(directory); }
    @Test void unreachableFailsWithinDeadline() {
        long t0 = System.nanoTime();
        assertThatThrownBy(() -> authorizer.info().await().atMost(Duration.ofSeconds(10))).isInstanceOf(StatusRuntimeException.class)
            .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode()).isIn(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED);
        assertThat(Duration.ofNanos(System.nanoTime() - t0)).isLessThan(Duration.ofSeconds(5));
    }
}
```

(`quarkus.topaz.devservices.enabled` is declared in Task 8. Until then omit that override; without a Dev Service the ports above are simply unreachable, which is what the test wants.)

- [ ] **Step 2: Run to verify it fails** (no bean of type `TopazClient`).
- [ ] **Step 3: Implement**

`TopazRecorder.createClient`: resolves `ResolvedServiceSettings` for both services (ports 8282/9292), named TLS via `registry.get(name)` or default via `registry.getDefault()`, global interceptors from `Arc.container().instance(GrpcClientInterceptorContainer.class)` when resolvable, `Vertx` from `Arc.container().instance(Vertx.class)`, builds the client, registers `shutdown.addShutdownTask(client::close)`. Deadline applied per call through `stub.withDeadlineAfter` inside the clients (Task 6 already takes `deadline`). `TopazProcessor` adds: `@Record(RUNTIME_INIT) @BuildStep` producing three `SyntheticBeanBuildItem`s (`TopazClient` with `.runtimeValue(...)`, `AuthorizerClient` and `DirectoryClient` via `recorder.authorizer(RuntimeValue<TopazClient>)` / `recorder.directory(...)`), `ExtensionSslNativeSupportBuildItem(FEATURE)`, `IndexDependencyBuildItem("org.sitenetsoft", "quarkus-topaz-grpc")`. Runtime pom: `quarkus-topaz-grpc`, `quarkus-grpc`, `quarkus-tls-registry`, `quarkus-vertx`, `quarkus-mutiny`. Deployment pom: the `-deployment` counterparts.

- [ ] **Step 4: Run to verify it passes.**
- [ ] **Step 5: Commit** `git commit -am "Expose TopazClient beans via recorder with TLS registry and global interceptors"`

---

### Task 8: Dev Services

**Files:**
- Create: `deployment/src/main/java/org/sitenetsoft/quarkus/topaz/deployment/TopazBuildTimeConfig.java` (`@ConfigRoot(phase = BUILD_TIME) @ConfigMapping(prefix = "quarkus.topaz")`: `HealthConfig health()` with `@WithDefault("true") boolean enabled()`, `TopazDevServicesConfig devservices()`)
- Create: `.../deployment/devservices/TopazDevServicesConfig.java`, `DevServicesTopazProcessor.java`, `TopazContainer.java`, `TopazConfigFile.java`, `TopazReadyWaitStrategy.java`, `TopazSeeder.java`, `LocationResolver.java`
- Create: `deployment/src/main/resources/topaz-devservices.properties` (`default.image=ghcr.io/aserto-dev/topaz:0.33.22`)
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/devservices/DevServicesTopazTest.java`, `DevServicesTopazApiKeyTest.java`, `DevServicesTopazNoPolicyTest.java`, `DevServicesTopazMissingManifestTest.java`, `DevServicesTopazDisabledWhenHostSetTest.java`
- Test resources: `deployment/src/test/resources/topaz/manifest.yaml` (the MANIFEST from Task 3), `topaz/policies/todo.rego`, `topaz/data/objects.json`, `topaz/data/relations.json`
- Delete: Task 3 spike test and resource.

**Interfaces:**
- Consumes: `TopazClient.builder()` (Task 6); Quarkus 3.40.1 `DevServicesResultBuildItem.owned()`, `StartableContainer`, `ContainerLocator.locateContainerWithLabels(8282, "quarkus-dev-service-topaz")`, `ComposeLocator`, `ConfigureUtil.configureSharedServiceLabel`, `DevServicesConfig`, `DockerStatusBuildItem`, `LaunchModeBuildItem`, `DevServicesComposeProjectBuildItem`, `DevServicesSharedNetworkBuildItem`, `OutputTargetBuildItem`.
- Produces: config group

```java
@ConfigGroup public interface TopazDevServicesConfig {
    @WithDefault("true") boolean enabled(); Optional<String> imageName(); @WithDefault("true") boolean shared(); @WithDefault("topaz") String serviceName();
    PolicyConfig policy();  // Optional<String> directory(); Optional<String> image();
    Optional<String> manifestLocation(); Optional<String> dataLocation(); Optional<String> apiKey();
    @ConfigDocMapKey("env-name") Map<String, String> containerEnv(); @WithDefault("true") boolean consoleEnabled();
}
```

The spec's `decision-logs.enabled` key is deliberately not implemented: Topaz writes no decision logs unless configured, so the key would do nothing (my call). The injected keys are `quarkus.topaz.authorizer.host`, `quarkus.topaz.authorizer.port`, `quarkus.topaz.directory.host`, `quarkus.topaz.directory.port`, `quarkus.topaz.plain-text=true`, `quarkus.topaz.api-key` (when set), `quarkus.topaz.console-url` (when console enabled).

Behaviour (spec section 6): skip when disabled, when `quarkus.topaz.authorizer.host` or `quarkus.topaz.directory.host` is user-set (`ConfigUtils.isPropertyNonEmpty`), or when no container runtime. Discovery before start. `TopazConfigFile.write(Path dir, TopazDevServicesConfig cfg)` produces the Task 3 config with `auth.keys: [apiKey]` and `enable_api_key: true, enable_anonymous: false` when `apiKey` is set (Info stays anonymous via the default overrides), `local_bundles.paths: ["/policies"]` when `policy.directory` is set, `opa.config.services.ghcr` + `opa.config.bundles.policy.resource: <image>` when `policy.image` is set, console section only when `consoleEnabled`. Files go under `OutputTargetBuildItem.getOutputDirectory().resolve("topaz-devservices")`. `LocationResolver.resolve(String location) -> Path` supports `classpath:` (copied to the same output dir) and `filesystem:` or bare paths; throws `ConfigurationException("quarkus.topaz.devservices.<key> points to a missing location: ...")`. `TopazReadyWaitStrategy` polls `authorizer().info()` and `directory().getManifest()` (NOT_FOUND counts as ready) through a `TopazClient` built with a throwaway `Vertx`, until `startupTimeout`. `TopazSeeder.seed(TopazClient, Optional<Path> manifest, Optional<Path> data)` runs in `postStartHook`: `setManifest` then `importData` from `objects.json` (`{"objects":[...]}`) and `relations.json` (`{"relations":[...]}`) parsed with `com.google.protobuf.util.JsonFormat`; on a discovered container it only compares manifests and logs `WARN` on drift. `serviceConfig` is the `TopazDevServicesConfig` record so live reload reuses the container.

- [ ] **Step 1: Write the test resources**

`todo.rego`:

```rego
package todoApp.GET.todos
import future.keywords.if
default allowed = false
allowed if { input.user.id != "" }
```

and a second package `todoApp.DELETE.todos.__id` whose `allowed` calls `ds.check({"object_type":"todo","object_id":input.resource.id,"relation":"owner","subject_type":"user","subject_id":input.user.id})`. `objects.json`: users `rick@the-citadel.com` and `morty@the-citadel.com` (type `user`, with `properties.email`), todo `todo-1`. `relations.json`: `todo:todo-1#owner@user:rick@the-citadel.com`.

- [ ] **Step 2: Write the failing `DevServicesTopazTest`**

```java
.overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
.overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
.overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
@Test void authorizerServes()   { assertThat(topaz.authorizer().info().await().atMost(ofSeconds(10)).getVersion()).isNotBlank(); }
@Test void manifestLoaded()     { assertThat(topaz.directory().getManifest().await()...).contains("can_delete: owner"); }
@Test void dataImported()       { assertThat(topaz.directory().check("todo","todo-1","owner","user","rick@the-citadel.com").await()...).isTrue();
                                  assertThat(topaz.directory().check("todo","todo-1","owner","user","morty@the-citadel.com").await()...).isFalse(); }
@Test void policyEvaluates()    { assertThat(topaz.authorizer().isAllowed(IdentityContexts.subject("rick@the-citadel.com"), "todoApp.GET.todos", "allowed", Map.of()).await()...).isTrue();
                                  assertThat(topaz.authorizer().isAllowed(IdentityContexts.subject("rick@the-citadel.com"), "todoApp.DELETE.todos.__id", "allowed", Map.of("id","todo-1")).await()...).isTrue();
                                  assertThat(topaz.authorizer().isAllowed(IdentityContexts.subject("morty@the-citadel.com"), "todoApp.DELETE.todos.__id", "allowed", Map.of("id","todo-1")).await()...).isFalse(); }
@Test void unknownPolicyPathPropagatesError() { assertThatThrownBy(() -> topaz.authorizer().isAllowed(subject("rick@the-citadel.com"), "todoApp.NOPE", "allowed", Map.of()).await()...).isInstanceOf(StatusRuntimeException.class); }
@Test void consoleUrlInjected() { assertThat(ConfigProvider.getConfig().getOptionalValue("quarkus.topaz.console-url", String.class)).isPresent(); }
```

- [ ] **Step 3: Run to verify it fails** (`quarkus.topaz.devservices.*` unknown, no container).
- [ ] **Step 4: Implement `TopazBuildTimeConfig`, `TopazDevServicesConfig`, `TopazConfigFile`, `LocationResolver`, `TopazContainer`, `TopazReadyWaitStrategy`, `TopazSeeder`, `DevServicesTopazProcessor`.**

Deployment pom adds `io.quarkus:quarkus-devservices-deployment`, `io.quarkus:quarkus-devservices-common`, `com.google.protobuf:protobuf-java-util`, `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` (BOM-managed) for writing the config. Runtime pom adds `io.quarkus:quarkus-devservices`.

- [ ] **Step 5: Run to verify it passes** with the Podman env vars.
- [ ] **Step 6: Delete the Task 3 spike test and resource; run the deployment tests again.**
- [ ] **Step 7: Write and pass the remaining tests**

- `DevServicesTopazApiKeyTest`: `devservices.api-key=secret` plus `quarkus.topaz.api-key=wrong` → `info()` succeeds, `getManifest()` fails with `Status.Code.UNAUTHENTICATED` (`wrongApiKeyIsUnauthenticated`); a second profile-free test class `DevServicesTopazApiKeyOkTest` with only `devservices.api-key=secret` → `getManifest()` succeeds.
- `DevServicesTopazNoPolicyTest`: only `manifest-location` set → `directory().getManifest()` works (`startsWithoutPolicy`); `info()` works.
- `DevServicesTopazMissingManifestTest`: `manifest-location=classpath:nope.yaml` with `.setExpectedException(ConfigurationException.class)` and `assertLogRecords` containing `quarkus.topaz.devservices.manifest-location` (`missingManifestFailsBuild`).
- `DevServicesTopazDisabledWhenHostSetTest`: `quarkus.topaz.authorizer.host=example.invalid`, `plain-text=true`, `deadline=1s` → no container started (assert `ConfigProvider` has no `quarkus.topaz.console-url`) and `info()` fails UNAVAILABLE.

- [ ] **Step 8: Commit** `git commit -am "Add Topaz Dev Service with generated config, policy mount, manifest and data seeding"`

---

### Task 9: Health check

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/health/TopazHealthCheck.java`
- Modify: `deployment/.../TopazProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/TopazHealthCheckTest.java`, `TopazHealthDownTest.java`, `TopazHealthDisabledTest.java`

**Interfaces:**
- Produces: `@Readiness @ApplicationScoped class TopazHealthCheck implements io.smallrye.health.api.AsyncHealthCheck` named `Topaz`, data `authorizer` = `host:port`, `directory` = `host:port`, `error` on failure; UP when `info()` succeeds and `getManifest()` succeeds or fails NOT_FOUND. Registered with `new HealthBuildItem(TopazHealthCheck.class.getName(), buildTimeConfig.health().enabled())` only when `capabilities.isPresent(Capability.SMALLRYE_HEALTH)`. Runtime pom: `quarkus-smallrye-health` `<optional>true</optional>`; deployment pom: `quarkus-smallrye-health-spi`, test-scoped `quarkus-smallrye-health-deployment`.

- [ ] **Step 1: Write the failing tests** (RestAssured `GET /q/health/ready`): `upWithDevService` → status 200, body `checks[?(@.name=='Topaz')].status == UP`; `downWhenUnreachable` (`authorizer.host=example.invalid`, `deadline=1s`, devservices disabled) → 503 and `Topaz` check `DOWN` with non-empty `data.error`; `disabledWhenConfigured` (`quarkus.topaz.health.enabled=false`) → no check named `Topaz`.
- [ ] **Step 2: Run to verify they fail.** 
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run to verify they pass.**
- [ ] **Step 5: Commit** `git commit -am "Add Topaz readiness health check"`

---

### Task 10: Global client interceptors (tracing hook) verified

**Files:**
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/GlobalInterceptorTest.java`
- Modify: `runtime/.../TopazRecorder.java` only if the test fails.

- [ ] **Step 1: Write the failing test**

Test archive contains `@GlobalInterceptor @ApplicationScoped class RecordingInterceptor implements ClientInterceptor` adding `method.getFullMethodName()` to a static `CopyOnWriteArrayList`. With the Dev Service running: `topaz.authorizer().info().await()...; topaz.directory().getManifest().await()...;` then `assertThat(RecordingInterceptor.CALLS).contains("aserto.authorizer.v2.Authorizer/Info", "aserto.directory.model.v3.Model/GetManifest")`.

- [ ] **Step 2: Run** `mvn -q -pl deployment test -Dtest=GlobalInterceptorTest`. Expected: PASS if Task 7 wired `GrpcClientInterceptorContainer`; otherwise fix the recorder until it passes.
- [ ] **Step 3: Commit** `git commit -am "Verify global gRPC client interceptors apply to Topaz channels"`

---

### Task 11: Dev UI card

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/topaz/runtime/devui/TopazJsonRpcService.java`
- Create: `deployment/src/main/java/org/sitenetsoft/quarkus/topaz/deployment/devui/TopazDevUIProcessor.java`
- Create: `deployment/src/main/resources/dev-ui/qwc-topaz-policies.js`, `qwc-topaz-manifest.js`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/topaz/deployment/devui/TopazDevUITest.java`

**Interfaces:**
- Produces: JSON-RPC methods `connection()` → `{authorizer, directory, consoleUrl}`, `listPolicies()` → array of `{id, packagePath}` from `Module.getId()`/`getPackagePath()`, `getManifest()` → string. Card (`CardPageBuildItem`, `@BuildStep(onlyIf = IsLocalDevelopment.class)`): external link page to `consoleUrl` when present (title `Topaz console`, icon `font-awesome-solid:shield-halved`), web component pages `Policies` and `Manifest`. Deployment pom: `io.quarkus:quarkus-devui-deployment-spi`; test: `io.quarkus:quarkus-vertx-http-dev-ui-tests`.

- [ ] **Step 1: Write the failing test** using `QuarkusDevModeTest` with the Task 8 resources and `new DevUIJsonRPCTest("org.sitenetsoft.quarkus-topaz")`: `listPoliciesReturnsTodoPackages` asserts the JSON array contains an entry whose `packagePath` starts with `todoApp.`; `manifestReturned` asserts the string contains `can_delete`.
- [ ] **Step 2: Run to verify it fails.** 
- [ ] **Step 3: Implement** (Lit components modelled on `~/Projects/SiteNetSoft/quarkus-tus/deployment/src/main/resources/dev-ui/qwc-tus-uploads.js`; table for policies, `<pre>` for the manifest).
- [ ] **Step 4: Run to verify it passes.**
- [ ] **Step 5: Commit** `git commit -am "Add Topaz Dev UI card with console link, policies and manifest pages"`

---

### Task 12: Integration tests module, including native

**Files:**
- Create: `integration-tests/pom.xml` (artifact `quarkus-topaz-integration-tests`, `quarkus-maven-plugin` `build` goal, deps `quarkus-topaz`, `quarkus-rest-jackson`, `quarkus-smallrye-health`, test `quarkus-junit5`, `rest-assured`; `native` profile per spec section 8 with `quarkus.native.enabled=true`, `quarkus.package.jar.enabled=false`, failsafe running `*IT`)
- Create: `integration-tests/src/main/java/org/sitenetsoft/quarkus/topaz/it/TodoResource.java`
- Create: `integration-tests/src/main/resources/application.properties`, `topaz/manifest.yaml`, `topaz/policies/todo.rego`, `topaz/data/objects.json`, `topaz/data/relations.json` (copies of Task 8 resources)
- Test: `integration-tests/src/test/java/org/sitenetsoft/quarkus/topaz/it/TodoResourceTest.java`, `TodoResourceIT.java`

**Interfaces:**
- Produces endpoints: `GET /todos/{id}/can-delete?user=<email>` → `{"allowed": bool}` from `authorizer.isAllowed(subject(user), "todoApp.DELETE.todos.__id", "allowed", Map.of("id", id))`, 403 when Topaz rejects the identity (catch `StatusRuntimeException` and return 403 with the status code in the body); `GET /todos/{id}/owner/{user}` → `{"owner": bool}` from `directory.check(...)`; `POST /todos/{id}/owner/{user}` → `directory.setRelation`.

- [ ] **Step 1: Write the failing `TodoResourceTest`** (`@QuarkusTest`): `rickCanDelete` 200 `allowed=true`; `mortyCannotDelete` 200 `allowed=false`; `unknownUserIs403`; `ownerCheck` true for rick false for morty; `setOwnerThenCheck` POST morty then check morty true; `readyIncludesTopaz` GET `/q/health/ready` has check `Topaz` UP.
- [ ] **Step 2: Run** `... mvn -q -pl integration-tests -am verify` → FAIL (no resource).
- [ ] **Step 3: Implement `TodoResource` and `application.properties`** (`quarkus.topaz.devservices.policy.directory=classpath:topaz/policies`, `manifest-location`, `data-location`).
- [ ] **Step 4: Run to verify it passes.**
- [ ] **Step 5: Add `TodoResourceIT extends TodoResourceTest` annotated `@QuarkusIntegrationTest`; commit; then run the native build once**

```bash
git add -A && git commit -m "Add integration tests with a todo sample application"
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 \
mvn -pl integration-tests verify -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=podman \
  -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8
```

Expected: `TodoResourceIT` passes against the native binary. Fix native registrations in `TopazProcessor` if the binary fails at runtime (typical cause: a protobuf class missing from the Jandex index).

- [ ] **Step 6: Commit any native fixes** `git commit -am "Fix native image registrations for Topaz stubs"` (skip if none).

---

### Task 13: Docs module, README, CI

**Files:**
- Create: `docs/pom.xml` (profile `docs` in root; `quarkus-config-doc-maven-plugin`, `asciidoctor-maven-plugin` as in `quarkus-openfga-client/docs/pom.xml`), `docs/antora.yml`, `docs/modules/ROOT/nav.adoc`, `docs/modules/ROOT/pages/{index,client,dev-services,health-and-tracing,dev-ui,native,testing,configuration}.adoc`, `docs/templates/includes/attributes.adoc`
- Create: `.github/workflows/ci.yml`, `.github/workflows/pages.yml` (adapted from quarkus-tus)
- Modify: `README.md`

- [ ] **Step 1: Write the pages.** `index`: what Topaz is, which artifact to add, 20-line quick start with the todo policy; `client`: `TopazClient` API table, identity contexts, `isAllowed` semantics (false when decision absent, errors propagate), blocking facade, builder outside CDI; `dev-services`: every `quarkus.topaz.devservices.*` key with the policy directory versus image choice, manifest and data formats, shared container drift, Podman note; `configuration`: `include::includes/quarkus-topaz.adoc[]` generated; `testing`: how this repo's tests run (env vars, `-T 4`, native args). Every config key named in a page must exist in the generated reference.
- [ ] **Step 2: Build the docs** `mvn -q -Pdocs -pl docs package` → `docs/target/generated-docs/index.html` exists; `grep -c "quarkus.topaz.devservices.policy.directory" docs/target/generated-docs/configuration.html` ≥ 1.
- [ ] **Step 3: Write `ci.yml`**: on push and PR; job `build` on `ubuntu-latest`, `actions/setup-java@v6` Temurin 25, `mvn -B -T 4 verify`; job `native` needing `build`, `mvn -B -pl integration-tests -am verify -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`; job `topaz-previous` running `mvn -B -pl deployment -am test -Dquarkus.topaz.devservices.image-name=ghcr.io/aserto-dev/topaz:0.33.21`. `pages.yml` builds `-Pdocs` and publishes `docs/target/generated-docs` to GitHub Pages. GitHub runners have Docker, so no Podman env vars there.
- [ ] **Step 4: Update `README.md`** with installation snippet (Maven and Gradle), the three-property Dev Service quick start, and links to docs and the design spec.
- [ ] **Step 5: Full build and commit**

Run: `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=... mvn -T 4 verify -Pdocs`
Expected: BUILD SUCCESS.

```bash
git add -A && git commit -m "Add Antora docs, README and CI workflows"
```

---

## Follow-up plans (not part of this document)

- `2026-xx-xx-quarkus-topaz-security.md`: spec section 5 (`quarkus-topaz-security`), consuming `TopazClient`, `AuthorizerClient.isAllowed`, `DirectoryClient.check`, `IdentityContexts`.
- `2026-xx-xx-quarkus-topaz-zanzibar.md`: spec section 7 (`quarkus-topaz-zanzibar`), consuming `DirectoryClient.check/setRelation/deleteRelation`.
