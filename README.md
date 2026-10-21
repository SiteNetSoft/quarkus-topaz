# quarkus-topaz

Quarkus extension for [Topaz](https://github.com/aserto-dev/topaz) (aserto-dev/topaz): a reactive gRPC client for the authorizer and directory, Dev Services, a readiness health check and a Dev UI card. Status: pre-release, nothing is published yet.

## Installation

Maven:

```xml
<dependency>
    <groupId>org.sitenetsoft</groupId>
    <artifactId>quarkus-topaz</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Gradle:

```groovy
implementation 'org.sitenetsoft:quarkus-topaz:0.1.0-SNAPSHOT'
```

Enforcement lives in a second artifact, `quarkus-topaz-security`: a `topaz` HTTP security policy for routes (policy paths derived from the request, e.g. `todoApp.DELETE.todos.__id`), `@TopazAuthorize` and `@TopazCheck` interceptors for CDI bean methods, and an opt-in `@PermissionsAllowed` bridge. Every error denies. See [Security](docs/modules/ROOT/pages/security.adoc).

```xml
<dependency>
    <groupId>org.sitenetsoft</groupId>
    <artifactId>quarkus-topaz-security</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Quarkus-zanzibar users can use `quarkus-topaz-zanzibar`: it supplies the `RelationshipManager` backed by the Topaz Directory, so `@FGARelation` and the other quarkus-zanzibar annotations are answered by Topaz. Adding it enables quarkus-zanzibar's filter, which denies unannotated REST endpoints by default. See [Zanzibar connector](docs/modules/ROOT/pages/zanzibar-connector.adoc).

```xml
<dependency>
    <groupId>org.sitenetsoft</groupId>
    <artifactId>quarkus-topaz-zanzibar</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick start

Put a Rego policy, a directory manifest and seed data under `src/main/resources/topaz/` and point the Dev Service at them:

```properties
quarkus.topaz.devservices.policy.directory=classpath:topaz/policies
quarkus.topaz.devservices.manifest-location=classpath:topaz/manifest.yaml
quarkus.topaz.devservices.data-location=classpath:topaz/data
```

Then inject the client and ask for a decision:

```java
@Inject
TopazClient topaz;

Uni<Boolean> allowed = topaz.authorizer().isAllowed(IdentityContexts.subject("rick@the-citadel.com"),
        "todoApp.DELETE.todos.__id", "allowed", Map.of("id", "todo-1"));
```

`quarkus dev` starts Topaz in a container, loads the manifest and data, and configures the client. The complete example is the todo application in [`integration-tests`](integration-tests/).

## Documentation

- [Documentation](docs/modules/ROOT/pages/index.adoc), built with `mvn package -Pdocs` into `docs/target/generated-docs/`
- [Design spec](docs/design/specs/2026-10-18-quarkus-topaz-design.md)

## Building

```shell
JAVA_HOME=/path/to/jdk-25 mvn -T 4 verify
```

With rootless Podman, add `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`. See [Testing](docs/modules/ROOT/pages/testing.adoc).

## Releasing

The release procedure, its secrets and the local rehearsal are in [Releasing](docs/modules/ROOT/pages/releasing.adoc).

## License

[Apache License 2.0](LICENSE)
