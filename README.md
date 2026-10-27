# quarkus-topaz

Quarkus extension for [Topaz](https://github.com/aserto-dev/topaz), the aserto-dev/topaz authorizer. It provides a reactive gRPC client for the authorizer and the directory. It also adds a Dev Service, a readiness health check and a Dev UI card.

## Installation

Maven:

```xml
<dependency>
    <groupId>org.sitenetsoft</groupId>
    <artifactId>quarkus-topaz</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'org.sitenetsoft:quarkus-topaz:0.1.0'
```

Enforcement lives in a second artifact, `quarkus-topaz-security`. It provides:

- A `topaz` HTTP security policy for routes. It derives the policy path from the request, such as `todoApp.DELETE.todos.__id`.
- `@TopazAuthorize` and `@TopazCheck` interceptors for CDI bean methods.
- An opt-in `@PermissionsAllowed` bridge.

Every error denies. See [Security](docs/modules/ROOT/pages/security.adoc).

```xml
<dependency>
    <groupId>org.sitenetsoft</groupId>
    <artifactId>quarkus-topaz-security</artifactId>
    <version>0.1.0</version>
</dependency>
```

Quarkus-zanzibar users can add `quarkus-topaz-zanzibar`. It supplies a `RelationshipManager` backed by the Topaz directory. Topaz then answers `@FGARelation` and the other quarkus-zanzibar annotations. The artifact also enables the quarkus-zanzibar filter, which by default denies REST endpoints without an annotation. See [Zanzibar connector](docs/modules/ROOT/pages/zanzibar-connector.adoc).

```xml
<dependency>
    <groupId>org.sitenetsoft</groupId>
    <artifactId>quarkus-topaz-zanzibar</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick start

Put a Rego policy, a directory manifest and seed data under `src/main/resources/topaz/`. Point the Dev Service at them:

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

- [Documentation](docs/modules/ROOT/pages/index.adoc). `mvn package -Pdocs` builds it into `docs/target/generated-docs/`.
- [Design spec](docs/design/specs/2026-10-18-quarkus-topaz-design.md)

## Building

```shell
JAVA_HOME=/path/to/jdk-25 mvn -T 4 verify
```

With rootless Podman, add `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`. See [Testing](docs/modules/ROOT/pages/testing.adoc).

## Releasing

[Releasing](docs/modules/ROOT/pages/releasing.adoc) describes the release procedure, its secrets and the local rehearsal.

## License

[Apache License 2.0](LICENSE)
