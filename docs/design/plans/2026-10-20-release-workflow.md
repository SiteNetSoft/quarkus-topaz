# Release workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pushing a `vX.Y.Z` tag publishes the eleven `org.sitenetsoft` artifacts, signed, to the Maven Central Portal and opens a GitHub release, with every precondition checked by scripts that also run locally.

**Architecture:** Three layers. The POMs gain the metadata Central requires, deploy skips for the non-published modules, and a `release` profile that adds sources, Javadoc and GPG signing. Two shell scripts under `bin/` hold every check (version/tag/README, and the staging layout) so they are testable without credentials. `.github/workflows/release.yml` wires them together in the order quarkus-tus uses: check, test, sign-and-stage, verify, bundle, upload, wait, release.

**Tech Stack:** Maven 3.9, `maven-source-plugin`, `maven-javadoc-plugin`, `maven-gpg-plugin`, GitHub Actions, the Central Portal publisher API (`curl`), bash.

**Spec:** `docs/design/specs/2026-10-20-release-workflow-design.md`.

## Global Constraints

- Quarkus `3.40.1`; `quarkus.version` property; `io.quarkus:quarkus-bom` imported; nothing else version-pinned except what the BOM does not manage. The BOM manages no Maven plugins, so `maven-source-plugin`, `maven-javadoc-plugin` and `maven-gpg-plugin` are pinned through root properties `source-plugin.version`, `javadoc-plugin.version`, `gpg-plugin.version`.
- GroupId `org.sitenetsoft`, version `0.1.0-SNAPSHOT` stays as is in this plan; no license headers in files.
- `maven.compiler.release=21`; build with `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`.
- Container runtime is rootless Podman: commands that start containers run with `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`.
- Maven parallelism at most `-T 4` locally; one heavy job at a time; no native build in this plan.
- Published artifact ids are exactly: `quarkus-topaz-parent`, `quarkus-topaz-grpc-parent`, `quarkus-topaz-grpc`, `quarkus-topaz`, `quarkus-topaz-deployment`, `quarkus-topaz-security-parent`, `quarkus-topaz-security`, `quarkus-topaz-security-deployment`, `quarkus-topaz-zanzibar-parent`, `quarkus-topaz-zanzibar`, `quarkus-topaz-zanzibar-deployment`. Nothing else is deployed.
- No credentials, keys or `<distributionManagement>` in any committed file. Signing only under `-Prelease`.
- Scripts: bash, `set -euo pipefail`, executable, in the style of `bin/generate.sh`.
- Logs and scratch files under `.superpowers/logs/` or `target/`, never `/tmp`. Throwaway GPG homes go under `target/`.

## Review Focus

1. A tag `v0.2.0` on a POM at `0.1.0` must fail before anything is built. Pinned by `bin/release-check.sh` test `tag mismatch` (Task 2).
2. A `-SNAPSHOT` version must never reach the Portal, tag or dispatch. Pinned by `release-check.sh` test `snapshot refused` (Task 2).
3. A jar whose `.asc`, `-sources.jar` or `-javadoc.jar` is missing must fail the staging check, not Central's validation. Pinned by `check-staging.sh` tests (Task 2).
4. A `maven-metadata.xml` left in the bundle must fail the staging check. Pinned by `check-staging.sh` test `metadata present` (Task 2).
5. A README that still says `0.1.0-SNAPSHOT` or `nothing is published yet` must fail the release. Pinned by `release-check.sh` test `readme snapshot` (Task 2).

---

### Task 1: Central metadata, deploy skips and the `release` profile

**Files:**
- Modify: `pom.xml` (root: metadata block after `<name>`, three plugin version properties, `pluginManagement` entries, new `release` profile)
- Modify: `grpc/pom.xml`, `grpc/stubs/pom.xml`, `security/pom.xml`, `security/runtime/pom.xml`, `security/deployment/pom.xml`, `zanzibar/pom.xml`, `zanzibar/runtime/pom.xml`, `zanzibar/deployment/pom.xml`, `runtime/pom.xml`, `deployment/pom.xml` (add `<description>`)
- Modify: `grpc/generator/pom.xml`, `integration-tests/pom.xml`, `docs/pom.xml` (add `<maven.deploy.skip>true</maven.deploy.skip>` property, as `security/no-rest-tests/pom.xml:16` already does)
- Modify: `grpc/stubs/pom.xml:14` (remove `maven.javadoc.skip`; the Javadoc plugin only runs under the release profile, so the default build is unchanged)

**Interfaces:**
- Produces: `mvn -Prelease deploy -DaltDeploymentRepository=staging::file://$PWD/target/staging-deploy` writes the eleven artifacts of the Global Constraints, each jar with `-sources.jar`, `-javadoc.jar`, and `.asc` for every `.jar` and `.pom`; env `MAVEN_GPG_PASSPHRASE` is the passphrase. Task 2's `check-staging.sh` consumes this layout.

- [ ] **Step 1: Add the metadata to the root POM**

After `<name>Topaz - Parent</name>` in `pom.xml` add `<description>Quarkus extensions for the Topaz authorizer and directory</description>`, `<url>https://github.com/SiteNetSoft/quarkus-topaz</url>`, a `<licenses>` block with `The Apache License, Version 2.0` / `https://www.apache.org/licenses/LICENSE-2.0.txt`, a `<developers>` block with id `jnbdz`, name `Jean-Nicolas Boulay Desjardins`, organization `SiteNetSoft`, organizationUrl `https://github.com/SiteNetSoft`, and an `<scm>` block with connection `scm:git:https://github.com/SiteNetSoft/quarkus-topaz.git`, developerConnection `scm:git:ssh://git@github.com/SiteNetSoft/quarkus-topaz.git`, url `https://github.com/SiteNetSoft/quarkus-topaz`.

- [ ] **Step 2: Add a `<description>` to every published module POM and deploy skips to the others**

One sentence each (wording is the implementer's). Add `<maven.deploy.skip>true</maven.deploy.skip>` to the `<properties>` of `grpc/generator/pom.xml`, `integration-tests/pom.xml` and `docs/pom.xml`.

- [ ] **Step 3: Add the `release` profile to the root POM**

Properties: `<source-plugin.version>`, `<javadoc-plugin.version>`, `<gpg-plugin.version>` set to the latest stable releases (check `https://repo1.maven.org/maven2/org/apache/maven/plugins/<plugin>/maven-metadata.xml`). `pluginManagement` entries for the three plugins with those versions. Profile `release` with: `maven-source-plugin` execution `attach-sources` goal `jar-no-fork`; `maven-javadoc-plugin` execution `attach-javadocs` goal `jar` with `<doclint>all,-missing</doclint>`; `maven-gpg-plugin` execution `sign-artifacts` phase `verify` goal `sign` with `<gpgArguments><arg>--pinentry-mode</arg><arg>loopback</arg></gpgArguments>`. In `grpc/stubs/pom.xml` replace `<maven.javadoc.skip>true</maven.javadoc.skip>` with `<doclint>none</doclint>` (generated code; a module property cannot be overridden from a parent profile, and the plugin is bound only in the profile anyway).

- [ ] **Step 4: Verify the layout without signing**

Run:
```bash
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -B -T 4 -q -DskipTests -Prelease -Dgpg.skip deploy \
  -DaltDeploymentRepository=staging::file://$PWD/target/staging-deploy > .superpowers/logs/release-stage.log 2>&1
find target/staging-deploy -name '*.jar' -o -name '*.pom' | sed 's#target/staging-deploy/org/sitenetsoft/##' | sort
```
Expected: exactly the eleven artifact directories; every jar artifact has `<id>-0.1.0-SNAPSHOT.jar`, `-sources.jar`, `-javadoc.jar`; no `quarkus-topaz-grpc-generator`, `-integration-tests`, `-no-rest-tests`, `-docs` directories. Fix any Javadoc error the run surfaces (the message names the file and line).

- [ ] **Step 5: Verify signing with a throwaway key**

```bash
export GNUPGHOME=$PWD/target/gnupg && rm -rf "$GNUPGHOME" && mkdir -m 700 "$GNUPGHOME"
gpg --batch --pinentry-mode loopback --passphrase '' --quick-gen-key 'release test <test@example.invalid>' default default never
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 MAVEN_GPG_PASSPHRASE='' mvn -B -T 4 -q -DskipTests -Prelease deploy \
  -DaltDeploymentRepository=staging::file://$PWD/target/staging-deploy > .superpowers/logs/release-sign.log 2>&1
find target/staging-deploy \( -name '*.jar' -o -name '*.pom' \) ! -exec test -f {}.asc \; -print
```
Expected: the last command prints nothing (every jar and POM has an `.asc`). Confirm `mvn -B -T 4 -q -DskipTests verify` (no profile) still passes with no GPG key present.

- [ ] **Step 6: Commit**

```bash
git add pom.xml */pom.xml */*/pom.xml
git commit -m "Add the Central POM metadata, deploy skips and a release profile that signs and attaches sources and Javadoc"
```

---

### Task 2: The two check scripts

**Files:**
- Create: `bin/release-check.sh`, `bin/check-staging.sh`
- Test: `bin/test-release-scripts.sh` (bash test harness; each case is a function that builds a fixture under `target/release-script-tests/` and asserts the exit code and a message substring)

**Interfaces:**
- Consumes: the Task 1 staging layout.
- Produces: `bin/release-check.sh [TAG]` prints `version=<v>` on its last stdout line and exits 0, or exits 1 with a `::error::` line; `bin/check-staging.sh STAGING_DIR [--unsigned]` exits 0 when the layout is complete, 1 with `::error::` lines otherwise. Task 3 calls both.

- [ ] **Step 1: Write the failing tests**

`bin/test-release-scripts.sh` with these cases (names are the function names):
- `release_check_ok`: a fixture copy of the repo's POM with version `0.1.0` and a README without `-SNAPSHOT` or `nothing is published yet`; `release-check.sh v0.1.0` exits 0 and prints `version=0.1.0`.
- `release_check_snapshot_refused`: version `0.1.0-SNAPSHOT`; exits 1; output contains `Refusing to release a SNAPSHOT`.
- `release_check_tag_mismatch`: version `0.1.0`, tag `v0.2.0`; exits 1; output contains `does not match`.
- `release_check_readme_snapshot`: version `0.1.0` but README contains `0.1.0-SNAPSHOT`; exits 1; output contains `README.md still`.
- `release_check_no_tag`: version `0.1.0`, no argument; exits 0.
- `staging_ok`: a synthetic directory with the eleven artifacts at `0.1.0` (empty files are enough: `.pom`, and for jars `.jar`, `-sources.jar`, `-javadoc.jar`, each with `.asc`); exits 0.
- `staging_unsigned_flag`: same without `.asc` files and `--unsigned`; exits 0.
- `staging_missing_signature`: one `.asc` removed; exits 1; output names the file.
- `staging_missing_javadoc`: one `-javadoc.jar` removed; exits 1; output names `javadoc`.
- `staging_metadata_present`: a `maven-metadata.xml` added; exits 1; output contains `maven-metadata`.
- `staging_extra_artifact`: a `quarkus-topaz-integration-tests` directory added; exits 1; output contains `unexpected`.
- `staging_missing_artifact`: `quarkus-topaz-zanzibar` removed; exits 1; output contains `missing`.

The scripts take the project root from `$(dirname "$0")/..` by default and accept `RELEASE_CHECK_ROOT` as an override so the tests can point them at fixtures. The version is read with `mvn -q -f "$ROOT/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout`; the test fixture is a minimal `pom.xml` with only groupId, artifactId, version and packaging `pom`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `bin/test-release-scripts.sh`
Expected: every case fails with "No such file" for the scripts.

- [ ] **Step 3: Write `bin/release-check.sh`**

Reads the version as above; fails on `*-SNAPSHOT` with `::error::Refusing to release a SNAPSHOT version (<v>).`; when `$1` is given fails unless `$1 == "v$VERSION"` with `::error::Tag <tag> does not match the POM version <v>.`; fails if `grep -qE -- '-SNAPSHOT|nothing is published yet' "$ROOT/README.md"` with `::error::README.md still advertises a snapshot.`; finally prints `version=$VERSION`.

- [ ] **Step 4: Write `bin/check-staging.sh`**

The expected artifact list is a bash array of the eleven ids. For each `.pom` and `.jar` under `STAGING_DIR/org/sitenetsoft`: the id (the path component after `org/sitenetsoft/`) must be in the list, else `::error::unexpected artifact <id>`; every id in the list must have a directory, else `::error::missing artifact <id>`; for ids whose packaging is jar (all but the four `-parent` ones) the main, `-sources` and `-javadoc` jars must exist; every artifact must carry the same version (the directory under the id), else `::error::mixed versions`; any `maven-metadata.xml*` fails; without `--unsigned`, every `.jar` and `.pom` needs `<file>.asc`. All errors are collected and printed; exit 1 if any.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `bin/test-release-scripts.sh`
Expected: `12 passed, 0 failed`. Then the real layout from Task 1 Step 5: `bin/check-staging.sh target/staging-deploy` exits 0, and `bin/release-check.sh` exits 1 (the POM is a SNAPSHOT).

- [ ] **Step 6: Commit**

```bash
git add bin/release-check.sh bin/check-staging.sh bin/test-release-scripts.sh
git commit -m "Add the release preflight and staging-layout checks with their test harness"
```

---

### Task 3: `.github/workflows/release.yml`

**Files:**
- Create: `.github/workflows/release.yml`
- Test: `actionlint` through Podman, plus the script runs from Task 2

**Interfaces:**
- Consumes: `bin/release-check.sh`, `bin/check-staging.sh`, the `release` profile.

- [ ] **Step 1: Write the workflow**

Follow the spec §5.5 step list exactly; the quarkus-tus `release.yml` at `~/Projects/SiteNetSoft/quarkus-tus/.github/workflows/release.yml` is the model for the upload, poll and release steps (copy its shell, substitute `quarkus-topaz`, `target/`, and the two scripts). Fixed values: trigger `push.tags: ['v*']` and `workflow_dispatch` input `publishing_type` (choice `USER_MANAGED` default, `AUTOMATIC`); `concurrency: { group: release, cancel-in-progress: false }`; `permissions: { contents: write }`; job `if: github.ref_type == 'tag' || github.ref == 'refs/heads/master'`; `actions/checkout@v7`, `actions/setup-java@v6` (Temurin 25, `cache: maven`), `actions/upload-artifact@v7` as in `ci.yml`. Steps: `bin/release-check.sh` (with `"$GITHUB_REF_NAME"` only when `github.ref_type == 'tag'`; parse `version=` into a step output); `mvn -B -T 4 verify`; `gpg --batch --import` of `SIGNING_KEY` with a `::error::SIGNING_KEY is not set` guard; `mvn -B -T 4 -DskipTests -Prelease deploy -DaltDeploymentRepository=staging::file://$PWD/target/staging-deploy` with `MAVEN_GPG_PASSPHRASE: ${{ secrets.SIGNING_PASSWORD }}`; `bin/check-staging.sh target/staging-deploy`; bundle (`find target/staging-deploy -name 'maven-metadata.xml*' -delete`, `zip -qr target/central-bundle.zip .` from inside the directory, print `unzip -l`); upload with `name=quarkus-topaz-${VERSION}`; poll; artifact `central-bundle` 30 days `if: always()`; `gh release create` draft unless `PUBLISHED`, title `quarkus-topaz ${VERSION}`.

- [ ] **Step 2: Lint**

Run: `podman run --rm -v "$PWD:/repo:ro,Z" -w /repo docker.io/rhysd/actionlint:latest -color`
Expected: no output (exit 0). Also `bash -n` on each inline script extracted by eye is unnecessary; actionlint runs shellcheck on `run:` blocks.

- [ ] **Step 3: Rehearse the shell steps locally**

From Task 1's signed staging directory: run the bundle commands by hand into `target/central-bundle.zip`. Then `unzip -l target/central-bundle.zip | grep -c '\.asc$'` must equal `find target/staging-deploy \( -name '*.jar' -o -name '*.pom' \) | wc -l`, and `unzip -l target/central-bundle.zip | grep -c maven-metadata` must be 0. Do not call the Portal.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "Add the tag-triggered release workflow that signs, stages, uploads to the Central Portal and drafts the GitHub release"
```

---

### Task 4: Releasing guide and README

**Files:**
- Create: `docs/modules/ROOT/pages/releasing.adoc`
- Modify: `docs/modules/ROOT/nav.adoc` (add `* xref:releasing.adoc[Releasing]` after Testing), `README.md` (add a "Releasing" line pointing at the page; leave the `0.1.0-SNAPSHOT` coordinates and the status sentence as they are, the release procedure replaces them)

- [ ] **Step 1: Write `releasing.adoc`**

Model: `~/Projects/SiteNetSoft/quarkus-tus/docs/modules/ROOT/pages/releasing.adoc`. Sections: the artifact table (eleven rows, spec §5.1, with the "ids are fixed" IMPORTANT note); "The release workflow" (numbered steps mirroring spec §5.5, naming `bin/release-check.sh` and `bin/check-staging.sh`); "Setting the version" with the exact commands:
```bash
mvn -Pdocs org.codehaus.mojo:versions-maven-plugin:2.18.0:set -DnewVersion=0.1.0 -DgenerateBackupPoms=false
sed -i 's/0.1.0-SNAPSHOT/0.1.0/g' README.md   # and drop the "nothing is published yet" sentence
bin/release-check.sh v0.1.0
git commit -am "Release 0.1.0" && git tag v0.1.0 && git push origin master v0.1.0
```
followed by the bump to `0.2.0-SNAPSHOT` with the same `versions:set`; "Secrets it needs" (table of the four secrets with the quarkus-tus wording, plus the namespace note); "Publishing type" (USER_MANAGED draft semantics, AUTOMATIC on dispatch); "Checking before you publish" (`mvn -T 4 install -DskipTests`, a sample app against `~/.m2`, then the Task 1 Step 4 deploy command with `-Dgpg.skip` and `bin/check-staging.sh target/staging-deploy --unsigned`). Use `{project-version}` where the docs already do.

- [ ] **Step 2: Build the docs**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -B -T 4 -q -Pdocs -DskipTests package -pl docs -am > .superpowers/logs/release-docs.log 2>&1; grep -c WARN .superpowers/logs/release-docs.log`
Expected: BUILD SUCCESS, no Asciidoctor warnings, `docs/target/generated-docs/releasing.html` exists.

- [ ] **Step 3: Commit**

```bash
git add docs/modules/ROOT/pages/releasing.adoc docs/modules/ROOT/nav.adoc README.md
git commit -m "Document the release procedure, its secrets and the local rehearsal"
```

---

## Before tagging 0.1.0 (user checklist, outside this plan)

- Set the four repository secrets; verify the `org.sitenetsoft` namespace on the Portal.
- Confirm the extension descriptors' `guide:` URL matches where `pages.yml` deploys.
- Review the parked findings in memory (pre-release parked findings) one last time.
- Follow `releasing.adoc` "Setting the version".
