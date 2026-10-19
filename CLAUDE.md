# quarkus-topaz

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

## Skills

- Quarkus practices: `~/Projects/SiteNetSoft/ai-skills/dev-practices/quarkus/index.md`
- Skill authoring: `~/Projects/SiteNetSoft/ai-skills/meta/skill-authoring/index.md`
