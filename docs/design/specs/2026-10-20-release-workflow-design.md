# Release workflow design

Date: 2026-10-20. Status: approved for planning (the user asked for the quarkus-tus defaults without a brainstorming round; every choice below is the author's call, modelled on `quarkus-tus/.github/workflows/release.yml` and its `releasing.adoc`).

## 1. Intent

Cutting 0.1.0 (and every later release) must be one action: set the version, tag `vX.Y.Z`, push the tag. A GitHub Actions workflow then builds and tests the tagged commit, signs every artifact, uploads a bundle to the Maven Central Portal, waits for Central's validation, and opens a GitHub release. Nothing about publishing lives on a developer machine: no credentials in the build, no manual `mvn deploy`.

## 2. Goals

- A `v*` tag push publishes exactly the artifacts consumers need and nothing else.
- The workflow refuses to publish a `-SNAPSHOT`, a tag that disagrees with the POM version, or a README that still advertises a snapshot.
- Every published jar has `-sources` and `-javadoc` companions and every jar and POM has a detached `.asc` signature, checked before upload so a rejection is caught while the log says why.
- The upload is held for review on the Portal (`USER_MANAGED`) by default; a manual dispatch may choose `AUTOMATIC`.
- The GitHub release is a draft until Central reports `PUBLISHED`, so the tag never advertises artifacts nobody can download.
- The same checks the workflow runs are shell scripts under `bin/` that run locally without credentials, so a release can be rehearsed.

## 3. Non-goals

- Registering the extensions in the Quarkus extension registry (`registry.quarkus.io`). A later plan.
- Rebuilding the native image in the release job. The tagged commit already passed `ci.yml` on master.
- Automated version bumping or changelog generation. The version is set by hand with `versions:set`; release notes are GitHub's generated notes.
- Publishing snapshots.
- Changing where the docs are hosted (`pages.yml` is untouched).

## 4. Anti-goals

- Credentials or key material in any committed file or in Maven settings in the repository.
- A workflow that goes green before Central has validated the deployment.
- A local developer build that starts requiring a GPG key: signing happens only under the `release` Maven profile.

## 5. Decisions

### 5.1 Published artifacts

Eleven artifacts under `org.sitenetsoft`, all at the reactor version:

| artifactId | packaging | why |
|---|---|---|
| `quarkus-topaz-parent` | pom | parent of every module |
| `quarkus-topaz-grpc-parent` | pom | parent of the stubs |
| `quarkus-topaz-grpc` | jar | generated stubs; runtime depends on it |
| `quarkus-topaz` | jar | client runtime |
| `quarkus-topaz-deployment` | jar | resolved by Quarkus from the runtime descriptor |
| `quarkus-topaz-security-parent` | pom | parent |
| `quarkus-topaz-security` | jar | |
| `quarkus-topaz-security-deployment` | jar | |
| `quarkus-topaz-zanzibar-parent` | pom | parent |
| `quarkus-topaz-zanzibar` | jar | |
| `quarkus-topaz-zanzibar-deployment` | jar | |

Not published (`maven.deploy.skip=true`): `quarkus-topaz-grpc-generator` (the proto generation app behind `bin/generate.sh`), `quarkus-topaz-integration-tests`, `quarkus-topaz-security-no-rest-tests` (already skipped), `quarkus-topaz-docs`.

The artifact ids are fixed: each runtime jar's `META-INF/quarkus-extension.properties` names its deployment artifact, so a rename breaks consumers at augmentation time.

### 5.2 Central-required POM metadata

The root POM carries, inherited by every module: `<name>` (each module already has one), `<description>`, `<url>https://github.com/SiteNetSoft/quarkus-topaz</url>`, Apache-2.0 `<licenses>`, the `jnbdz` developer entry (same as quarkus-tus), and `<scm>` with `scm:git:https://github.com/SiteNetSoft/quarkus-topaz.git`, `scm:git:ssh://git@github.com/SiteNetSoft/quarkus-topaz.git`, and the repository URL. Every published module gets a one-sentence `<description>`.

### 5.3 The `release` Maven profile

Activated with `-Prelease`, defined in the root POM. It adds `maven-source-plugin` (`jar-no-fork`), `maven-javadoc-plugin` (`jar`), and `maven-gpg-plugin` (`sign` at `verify`, `--pinentry-mode loopback`, passphrase from `MAVEN_GPG_PASSPHRASE`). Plugin versions are pinned through root properties because the Quarkus BOM manages no plugins. The stubs module currently skips Javadoc; under the profile it produces one with `doclint` off, since Central requires a `-javadoc` jar for every jar. Outside the profile nothing changes for local builds.

There is no `<distributionManagement>`. The workflow deploys to a file repository with `-DaltDeploymentRepository=staging::file://$PWD/target/staging-deploy`; that layout, minus the `maven-metadata.xml*` files, zipped, is the Portal bundle.

### 5.4 Local checks (shell, no credentials)

- `bin/release-check.sh [TAG]`: prints the reactor version; fails on `-SNAPSHOT`; when TAG is given fails unless `TAG` equals `v<version>`; fails if `README.md` contains `-SNAPSHOT` or `nothing is published yet`.
- `bin/check-staging.sh STAGING_DIR [--unsigned]`: fails unless the directory holds exactly the eleven artifacts of 5.1 at the reactor version; fails if any jar lacks `-sources.jar` or `-javadoc.jar`; fails if any `maven-metadata.xml*` is present; unless `--unsigned`, fails if any `.jar` or `.pom` lacks a sibling `.asc`.

### 5.5 The workflow `.github/workflows/release.yml`

Trigger: `push.tags: ['v*']` and `workflow_dispatch` with input `publishing_type` (`USER_MANAGED` default, `AUTOMATIC`). Concurrency group `release`, no cancel. Permissions `contents: write`. Job condition: tag, or dispatch from `refs/heads/master`. Steps, in order:

1. checkout; Temurin 25 with Maven cache.
2. `bin/release-check.sh "$GITHUB_REF_NAME"` on a tag, no argument on dispatch; exports `version`.
3. `mvn -B -T 4 verify` (JVM tests, Dev Services on the runner's Docker).
4. Import `SIGNING_KEY` into gpg (`--batch --import`); fail with a clear message if the secret is empty.
5. `mvn -B -T 4 -DskipTests -Prelease deploy -DaltDeploymentRepository=staging::file://$PWD/target/staging-deploy` with `MAVEN_GPG_PASSPHRASE` from `SIGNING_PASSWORD`.
6. `bin/check-staging.sh target/staging-deploy`.
7. Delete `maven-metadata.xml*`, zip to `target/central-bundle.zip`, print the listing.
8. Upload to `https://central.sonatype.com/api/v1/publisher/upload?name=quarkus-topaz-<version>&publishingType=<type>` with a Bearer token built from `CENTRAL_USERNAME:CENTRAL_PASSWORD`; capture the deployment id.
9. Poll `/api/v1/publisher/status?id=` every 15 s, up to 40 times: `PUBLISHED` → exit 0 with state output; `VALIDATED` → warning, exit 0; `FAILED` → error. Timeout is an error.
10. Always upload the bundle as a workflow artifact, 30 days.
11. On a tag: `gh release create <tag> --title "quarkus-topaz <version>" --generate-notes`, `--draft` unless state is `PUBLISHED`.

### 5.6 Documentation

A `releasing.adoc` page in the Antora module (added to `nav.adoc` after Testing), modelled on quarkus-tus: the artifact table, the workflow's steps, the secrets table (`SIGNING_KEY`, `SIGNING_PASSWORD`, `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`), the version procedure (`versions:set` with `-Pdocs` so the profile module is updated too, README versions, commit, tag, push, then bump to the next `-SNAPSHOT`), the publishing-type semantics, and the local rehearsal (`mvn install`, a sample app against `~/.m2`, `deploy` to the staging directory plus `bin/check-staging.sh --unsigned`). README's status sentence and the three `0.1.0-SNAPSHOT` coordinates are replaced at release time; the releasing page says so and `bin/release-check.sh` enforces it.

## 6. Prerequisites outside the repository (user)

- Repository secrets `SIGNING_KEY` (ASCII-armoured secret key whose public half is on `keyserver.ubuntu.com` or `keys.openpgp.org`), `SIGNING_PASSWORD`, `CENTRAL_USERNAME`, `CENTRAL_PASSWORD` (a Portal user token).
- The `org.sitenetsoft` namespace verified on the Central Portal for that token's account.
- Decide whether the extension descriptors' `guide:` URL (`https://sitenetsoft.github.io/quarkus-topaz/`) is where `pages.yml` actually deploys; quarkus-tus uses `https://sitenetsoft.org/quarkus-tus/`. Not changed by this plan.

## 7. Left to the builder

Exact plugin versions (latest stable at implementation time, recorded in root properties), the wording of the module descriptions, the shell style of the two scripts (bash, `set -euo pipefail`, mirroring `bin/generate.sh`).
