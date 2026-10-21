#!/usr/bin/env bash
# Preflight for a release: refuses SNAPSHOT versions, tag/POM mismatches and a README that still advertises a snapshot.
# Usage: bin/release-check.sh [TAG]   (prints version=<v> as the last stdout line on success)
# Requires: mvn, JAVA_HOME pointing at a JDK 21+. RELEASE_CHECK_ROOT overrides the project root.
set -euo pipefail

ROOT="${RELEASE_CHECK_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
TAG="${1:-}"

VERSION="$(mvn -q -f "${ROOT}/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout)"

fail=0
if [[ "${VERSION}" == *-SNAPSHOT ]]; then
  echo "::error::Refusing to release a SNAPSHOT version (${VERSION})."
  fail=1
fi
if [[ -n "${TAG}" && "${TAG}" != "v${VERSION}" ]]; then
  echo "::error::Tag ${TAG} does not match the POM version ${VERSION}."
  fail=1
fi
if grep -qE -- '-SNAPSHOT|nothing is published yet' "${ROOT}/README.md"; then
  echo "::error::README.md still advertises a snapshot."
  fail=1
fi
[[ "${fail}" -eq 0 ]] || exit 1

echo "version=${VERSION}"
